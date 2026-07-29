package dev.ftcplus.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;


public final class AstTool {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final JavaParser PARSER = new JavaParser();

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            try {
                JsonObject req = JsonParser.parseString(line).getAsJsonObject();
                String op = req.get("op").getAsString();
                JsonObject result = dispatch(op, req);
                result.addProperty("op", op);
                System.out.println(GSON.toJson(result));
            } catch (Exception e) {
                JsonObject err = new JsonObject();
                err.addProperty("success", false);
                err.addProperty("error", e.getMessage());
                System.out.println(GSON.toJson(err));
            }

            System.out.flush();
        }
    }

    private static JsonObject dispatch(String op, JsonObject req) throws Exception {
        return switch (op) {
            case "parse"          -> parse(req);
            case "add-state"      -> addState(req);
            case "remove-state"   -> removeState(req);
            case "add-field"      -> addField(req);
            case "remove-field"   -> removeField(req);
            case "add-control"    -> addControl(req);
            case "remove-control" -> removeControl(req);
            case "add-param"      -> addParam(req);
            case "remove-param"   -> removeParam(req);
            case "set-hardware-entry" -> setHardwareEntry(req);
            default -> error("unknown op: " + op);
        };
    }


    private static JsonObject parse(JsonObject req) throws Exception {
        String filePath = req.get("file").getAsString();
        CompilationUnit cu = parsefile(filePath);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);

        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (classOpt.isEmpty()) {
            result.addProperty("success", false);
            result.addProperty("erro", "No class found in file");
            return result;
        }

        ClassOrInterfaceDeclaration cls = classOpt.get();
        result.addProperty("className", cls.getNameAsString());

        var fields = cls.getFields().stream()
                .flatMap(f -> f.getVariables().stream().map(v -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("name", v.getNameAsString());
                    o.addProperty("type", f.getElementType().asString());
                    v.getInitializer().ifPresent(i -> o.addProperty("init", i.toString()));
                    return o;
                }))
                .collect(Collectors.toList());
        result.add("fields", GSON.toJsonTree(fields));

        var states = cls.findAll(EnumDeclaration.class).stream()
                .flatMap(e -> e.getEntries().stream().map(entry -> entry.getNameAsString()))
                .collect(Collectors.toList());
        result.add("states", GSON.toJsonTree(states));

        var methods = cls.getMethods().stream().map(m -> {
            JsonObject o = new JsonObject();
            o.addProperty("name", m.getNameAsString());
            o.addProperty("returnType", m.getType().asString());
            o.add("params", GSON.toJsonTree(
                    m.getParameters().stream().map(p -> p.getTypeAsString() + " " + p.getTypeAsString())
                            .collect(Collectors.toList())
            ));
            return o;
        }).collect(Collectors.toList());
        result.add("methods", GSON.toJsonTree(methods));

        var annotations = cls.getAnnotations().stream()
                .map(a -> a.getNameAsString())
                .collect(Collectors.toList());
        result.add("annotations", GSON.toJsonTree(annotations));

        return result;
    }

    private static JsonObject addState(JsonObject req) throws Exception {
        String filePath = req.get("file").getAsString();
        String stateName = req.get("state").getAsString();

        CompilationUnit cu = parsefile(filePath);

        Optional<EnumDeclaration> enumOpt = cu.findFirst(EnumDeclaration.class,
                e -> e.getNameAsString().equals("State"));

        if (enumOpt.isEmpty()) {
            return error("No State enum found in " + filePath);
        }

        EnumDeclaration stateEnum = enumOpt.get();

        boolean exists = stateEnum.getEntries().stream()
                .anyMatch(e -> e.getNameAsString().equals(stateName));
        if (exists) {
            return error("State " + stateName + " already exists");
        }

        stateEnum.addEnumConstant(stateName);

        addDefineStatesStub(cu, stateName);

        writeFile(filePath, cu);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("state", stateName);
        return result;
    }

    private static void addDefineStatesStub(CompilationUnit cu, String stateName) {
        cu.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("defineStates"))
                .ifPresent(m -> m.getBody().ifPresent(body -> {
                    String stub = "states.state(State." + stateName + ")\n" +
                            "        .onEnter(() -> { /* TODO */ });";
                    body.addStatement(new ExpressionStmt(
                            new MethodCallExpr("/* " + stub + " */")
                    ));
                }));
    }


    private static JsonObject removeState(JsonObject req) throws Exception {
        String filePath = req.get("file").getAsString();
        String stateName = req.get("state").getAsString();

        CompilationUnit cu = parsefile(filePath);

        Optional<EnumDeclaration> enumOpt = cu.findFirst(EnumDeclaration.class,
                e -> e.getNameAsString().equals("State"));

        if (enumOpt.isEmpty()) return error("No State enum found");

        EnumDeclaration stateEnum = enumOpt.get();
        boolean removed = stateEnum.getEntries().removeIf(
                e -> e.getNameAsString().equals(stateName)
        );

        if (!removed) return error("State " + stateName + " not found");

        writeFile(filePath, cu);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        return result;
    }


    private static JsonObject addField(JsonObject req) throws Exception {
        String filePath  = req.get("file").getAsString();
        String type      = req.get("type").getAsString();
        String name      = req.get("name").getAsString();
        String init      = req.has("init") ? req.get("init").getAsString() : null;
        String modifier  = req.has("modifier") ? req.get("modifier").getAsString() : "private final";

        CompilationUnit cu = parsefile(filePath);

        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (classOpt.isEmpty()) return error("No class found");

        ClassOrInterfaceDeclaration cls = classOpt.get();

        boolean exists = cls.getFields().stream()
                .flatMap(f -> f.getVariables().stream())
                .anyMatch(v -> v.getNameAsString().equals(name));
        if (exists) return error("Field " + name + " already exists");

        FieldDeclaration field;
        if (init != null) {
            field = cls.addFieldWithInitializer(type, name,
                    new NameExpr(init), getModifiers(modifier));
        } else {
            field = cls.addField(type, name, getModifiers(modifier));
        }

        writeFile(filePath, cu);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("field", name);
        return result;
    }


    private static JsonObject removeField(JsonObject req) throws Exception {
        String filePath = req.get("file").getAsString();
        String name     = req.get("name").getAsString();

        CompilationUnit cu = parsefile(filePath);

        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (classOpt.isEmpty()) return error("No class found");

        ClassOrInterfaceDeclaration cls = classOpt.get();
        boolean removed = cls.getFields().removeIf(f ->
                f.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(name))
        );

        if (!removed) return error("Field " + name + " not found");

        writeFile(filePath, cu);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        return result;
    }


    private static JsonObject addControl(JsonObject req) throws Exception {
        String filePath  = req.get("file").getAsString();
        String button    = req.get("button").getAsString();
        String signal    = req.get("signal").getAsString();
        String trigger   = req.has("trigger") ? req.get("trigger").getAsString() : "whenPressed";

        CompilationUnit cu = parsefile(filePath);

        Optional<MethodDeclaration> configOpt = cu.findFirst(MethodDeclaration.class,
                m -> m.getNameAsString().equals("configure"));

        if (configOpt.isEmpty()) return error("No configure() method found");

        MethodDeclaration configure = configOpt.get();
        configure.getBody().ifPresent(body -> {
            String stmt = "controls()." + trigger + "(GamepadButton." + button + ")" +
                    ".send(" + signal + "::new);";
            body.addStatement(new ExpressionStmt(new NameExpr(stmt)));
        });

        writeFile(filePath, cu);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        return result;
    }


    private static JsonObject removeControl(JsonObject req) throws Exception {
        String filePath = req.get("file").getAsString();
        String button   = req.get("button").getAsString();

        CompilationUnit cu = parsefile(filePath);

        Optional<MethodDeclaration> configOpt = cu.findFirst(MethodDeclaration.class,
                m -> m.getNameAsString().equals("configure"));

        if (configOpt.isEmpty()) return error("No configure() method found");

        configOpt.get().getBody().ifPresent(body ->
                body.getStatements().removeIf(s -> s.toString().contains("GamepadButton." + button))
        );

        writeFile(filePath, cu);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        return result;
    }


    private static JsonObject addParam(JsonObject req) throws Exception {
        String filePath = req.get("file").getAsString();
        String type     = req.get("type").getAsString();
        String name     = req.get("name").getAsString();

        CompilationUnit cu = parsefile(filePath);

        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (classOpt.isEmpty()) return error("No class found");

        ClassOrInterfaceDeclaration cls = classOpt.get();

        cls.addFieldWithInitializer(type, name, new NameExpr(name),
                Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);

        Optional<ConstructorDeclaration> ctorOpt = cls.getConstructors().stream().findFirst();
        if (ctorOpt.isPresent()) {
            ConstructorDeclaration ctor = ctorOpt.get();
            ctor.addParameter(type, name);
            ctor.getBody().addStatement(
                    new ExpressionStmt(new AssignExpr(
                            new FieldAccessExpr(new ThisExpr(), name),
                            new NameExpr(name),
                            AssignExpr.Operator.ASSIGN
                    ))
            );
        } else {
            ConstructorDeclaration ctor = cls.addConstructor(Modifier.Keyword.PUBLIC);
            ctor.addParameter(type, name);
            ctor.getBody().addStatement(
                    new ExpressionStmt(new AssignExpr(
                            new FieldAccessExpr(new ThisExpr(), name),
                            new NameExpr(name),
                            AssignExpr.Operator.ASSIGN
                    ))
            );
        }

        writeFile(filePath, cu);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        return result;
    }


    private static JsonObject removeParam(JsonObject req) throws Exception {
        String filePath = req.get("file").getAsString();
        String name     = req.get("name").getAsString();

        CompilationUnit cu = parsefile(filePath);

        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (classOpt.isEmpty()) return error("No class found");

        ClassOrInterfaceDeclaration cls = classOpt.get();

        cls.getFields().removeIf(f ->
                f.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(name))
        );

        cls.getConstructors().forEach(ctor -> {
            ctor.getParameters().removeIf(p -> p.getNameAsString().equals(name));
            ctor.getBody().getStatements().removeIf(s ->
                    s.toString().contains("this." + name)
            );
        });

        writeFile(filePath, cu);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        return result;
    }


    private static JsonObject setHardwareEntry(JsonObject req) throws Exception {
        String filePath = req.get("file").getAsString();
        String entry    = req.get("entry").getAsString();

        CompilationUnit cu = parsefile(filePath);

        Optional<ConstructorDeclaration> ctorOpt = cu.findFirst(ConstructorDeclaration.class);
        if (ctorOpt.isEmpty()) return error("No constructor found");

        ConstructorDeclaration ctor = ctorOpt.get();
        ctor.getBody().findFirst(ExplicitConstructorInvocationStmt.class).ifPresent(superCall -> {
            if (!superCall.getArguments().isEmpty()) {
                superCall.getArguments().set(0, new NameExpr("Hardware." + entry));
            }
        });

        writeFile(filePath, cu);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        return result;
    }


    private static CompilationUnit parsefile(String filePath) throws Exception {
        ParseResult<CompilationUnit> result = PARSER.parse(Paths.get(filePath));
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            throw new Exception("Failed to parse " + filePath + ": " +
                    result.getProblems().stream().map(Object::toString)
                            .collect(Collectors.joining(", ")));
        }
        return result.getResult().get();
    }

    private static void writeFile(String filePath, CompilationUnit cu) throws IOException {
        String source = new DefaultPrettyPrinter().print(cu);
        Files.writeString(Paths.get(filePath), source);
    }

    private static Modifier.Keyword[] getModifiers(String modifier) {
        return switch (modifier.trim()) {
            case "private final"    -> new Modifier.Keyword[]{Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL};
            case "private"          -> new Modifier.Keyword[]{Modifier.Keyword.PRIVATE};
            case "public"           -> new Modifier.Keyword[]{Modifier.Keyword.PUBLIC};
            case "public final"     -> new Modifier.Keyword[]{Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL};
            case "protected"        -> new Modifier.Keyword[]{Modifier.Keyword.PROTECTED};
            default                 -> new Modifier.Keyword[]{Modifier.Keyword.PRIVATE};
        };
    }

    private static JsonObject error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", false);
        obj.addProperty("error", message);
        return obj;
    }

}