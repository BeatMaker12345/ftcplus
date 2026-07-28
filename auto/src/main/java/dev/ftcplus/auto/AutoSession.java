package dev.ftcplus.auto;

import com.pedropathing.follower.Follower;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.IntFunction;

public class AutoSession {
    private final Robot<?, ?, ?> robot;
    private final Runtime runtime;
    private final Follower follower;

    private final Deque<ExecutionFrame> stack = new ArrayDeque<>();

    private AutoAction currentAction = null;
    private boolean actionStarted = false;

    public AutoSession(Robot<?, ?, ?> robot, Runtime runtime, Follower follower, AutoPath path) {
        this.robot    = robot;
        this.runtime  = runtime;
        this.follower = follower;
        path.define();
        stack.push(new SequenceFrame(path.steps()));
    }

    public void update() {
        if (isFinished()) return;

        runtime.update();
        follower.update();

        if (currentAction != null) {
            currentAction.onUpdate();
            if (currentAction.isFinished()) {
                currentAction.onEnd();
                currentAction = null;
                actionStarted = false;
            } else {
                return;
            }
        }

        while (!stack.isEmpty()) {
            AutoAction next = advance(stack.peek());
            if (next != null) {
                currentAction = next;
                currentAction.attach(robot, follower);
                currentAction.onStart();
                actionStarted = true;
                return;
            }
            stack.pop();
        }
    }

    public boolean isFinished() {
        return stack.isEmpty() && currentAction == null;
    }

    private AutoAction advance(ExecutionFrame frame) {
        if (frame instanceof SequenceFrame) {
            return advanceSequence((SequenceFrame) frame);
        } else if (frame instanceof  RepeatFrame) {
            return advanceRepeat((RepeatFrame) frame);
        }
        return null;
    }

    private AutoAction advanceSequence(SequenceFrame frame) {
        while (frame.index < frame.steps.size()) {
            AutoStep step = frame.steps.get(frame.index++);

            if (step instanceof ActionStep) {
                return ((ActionStep) step).action;
            }

            if (step instanceof BranchStep) {
                BranchStep branch = (BranchStep) step;
                for (BranchStep.Case c : branch.cases) {
                    if (c.evaluate()) {
                        if (c.action != null) return c.action;
                        if (c.subPath != null) {
                            c.subPath.define();
                            stack.push(new SequenceFrame(c.subPath.steps()));
                            return advance(stack.peek());
                        }
                        break;
                    }
                }
            }

            if (step instanceof RepeatStep) {
                RepeatStep repeat = (RepeatStep) step;
                stack.push(new RepeatFrame(repeat));
                return advance(stack.peek());
            }
        }
        return null;
    }

    private AutoAction advanceRepeat(RepeatFrame frame) {
        RepeatStep repeat = frame.repeat;

        if (repeat.isTimeBased()) {
            if (System.currentTimeMillis() - frame.startTime >= repeat.durationMs) {
                return null;
            }
        } else {
            if (frame.iteration >= repeat.count) {
                return null;
            }
        }

        if (frame.actionIndex < repeat.actionFactories.size()) {
            IntFunction<AutoAction> factory = repeat.actionFactories.get(frame.actionIndex);
            AutoAction action = factory.apply(frame.iteration);

            if (repeat.incrementPerAction) {
                frame.actionIndex++;
                frame.iteration++;
            } else {
                frame.actionIndex++;
                if (frame.actionIndex >= repeat.actionFactories.size()) {
                    frame.actionIndex = 0;
                    frame.iteration++;
                }
            }

            return action;
        }

        return null;
    }


    private interface ExecutionFrame {}

    private static final class SequenceFrame implements ExecutionFrame {
        final List<AutoStep> steps;
        int index = 0;

        SequenceFrame(List<AutoStep> steps) { this.steps = steps; }
    }

    private static final class RepeatFrame implements ExecutionFrame {
        final RepeatStep repeat;
        int iteration   = 0;
        int actionIndex = 0;
        final long startTime = System.currentTimeMillis();

        RepeatFrame(RepeatStep repeat) { this.repeat = repeat; }
    }
}