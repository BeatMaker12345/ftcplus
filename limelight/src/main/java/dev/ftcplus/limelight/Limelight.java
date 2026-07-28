package dev.ftcplus.limelight;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import dev.ftcplus.core.HardwareEntry;
import dev.ftcplus.core.sensor.Sensor;
import dev.ftcplus.core.signal.Event;
import dev.ftcplus.limelight.signal.AprilTagDetected;
import dev.ftcplus.limelight.signal.TargetDetected;
import dev.ftcplus.limelight.signal.TargetLost;
import dev.ftcplus.limelight.signal.TargetObservation;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;

import java.util.ArrayList;
import java.util.List;

public abstract class Limelight extends Sensor {
    private final HardwareEntry entry;
    private Limelight3A ll;

    private boolean publishing = false;
    private boolean wasTargetVisible = false;
    private double imuOffset = 0.0;

    private final List<AprilTagBinding> aprilTagBindings = new ArrayList<>();

    protected Limelight(HardwareEntry entry) {
        this.entry = entry;
    }

    @Override
    protected final void onInitialize() {
        onLimelightInitialize();
    }

    protected void onLimelightInitialize() {}

    @Override
    protected final void onUpdate() {
        if (ll == null) return;

        LLResult result = ll.getLatestResult();
        if (result == null) return;

        boolean hasTarget = result.isValid();

        if (publishing) {
            if (hasTarget && !wasTargetVisible) {
                send(new TargetDetected(
                        result.getTx(), result.getTy()
                ));
            } else if (!hasTarget && wasTargetVisible) {
                send(new TargetLost());
            }

            if (hasTarget) {
                send(new TargetObservation(
                        result.getTx(),
                        result.getTy(),
                        result.getTa(),
                        result.getTa(),
                        ll.getStatus().getPipelineIndex()
                ));
            }
        }

        wasTargetVisible = hasTarget;

        if (!aprilTagBindings.isEmpty() && result.getFiducialResults() != null) {
            for (AprilTagBinding binding : aprilTagBindings) {
                boolean tagVisible = false;
                AprilTagDetected detected = null;

                for (LLResultTypes.FiducialResult tag : result.getFiducialResults()) {
                    if (tag.getFiducialId() == binding.tagId) {
                        tagVisible = true;
                        Pose3D botpose = tag.getRobotPoseTargetSpace();
                        detected = new AprilTagDetected(
                                tag.getFiducialId(),
                                botpose != null ? botpose.getPosition().x : 0,
                                botpose != null ? botpose.getPosition().y : 0,
                                botpose != null ? botpose.getPosition().z : 0,
                                tag.getTargetXDegrees(),
                                tag.getTargetArea(),
                                Math.sqrt(
                                        Math.pow(botpose != null ? botpose.getPosition().x : 0, 2) +
                                        Math.pow(botpose != null ? botpose.getPosition().z : 0, 2)
                                )
                        );
                        break;
                    }
                }

                if (binding.shouldFire(tagVisible) && detected != null) {
                    Event signal = binding.signalFactory.apply(detected);
                    if (signal != null) send(signal);
                }
            }
        }
    }


    public void setPipeline(int index) {
        if (ll != null) ll.pipelineSwitch(index);
    }

    public void setThrottle(double fps) {
        if (ll != null) ll.setPollRateHz((int) fps);
    }

    public void startPublishing() { this.publishing = true; }
    public void stopPublishing()  { this.publishing = false; }

    public void snapshotInput()  { if (ll != null) ll.captureSnapshot("input"); }
    public void snapshotOutput() { if (ll != null) ll.captureSnapshot("output"); }

    public double getLatencyMs() {
        if (ll == null) return 0;
        LLResult r = ll.getLatestResult();
        return r != null ? r.getStaleness() : 0;
    }

    public boolean hasTarget() {
        if (ll == null) return false;
        LLResult r = ll.getLatestResult();
        return r != null && r.isValid();
    }


    public LimelightPose getBotpose() {
        if (ll == null) return null;
        LLResult r = ll.getLatestResult();
        if (r == null || !r.isValid()) return null;

        Pose3D pose = r.getBotpose_MT2();
        if (pose == null) return null;

        double correctedYaw = pose.getOrientation().getYaw(AngleUnit.DEGREES) + imuOffset;
        while (correctedYaw > 180)  correctedYaw -= 360;
        while (correctedYaw < -180) correctedYaw += 360;

        return new LimelightPose(
                pose.getPosition().x,
                pose.getPosition().y,
                pose.getPosition().z,
                correctedYaw,
                pose.getOrientation().getPitch(AngleUnit.DEGREES),
                pose.getOrientation().getRoll(AngleUnit.DEGREES),
                getLatencyMs(),
                r.getFiducialResults() != null ? r.getFiducialResults().size() : 0
        );
    }

    public void setImuOffset(double degrees) { this.imuOffset = degrees; }

    public AprilTagBindingBuilder onAprilTag(int tagId) {
        return new AprilTagBindingBuilder(tagId, this);
    }

    void addAprilTagBinding(AprilTagBinding binding) {
        aprilTagBindings.add(binding);
    }


    public final void attachLimelight(Limelight3A limelight) {
        this.ll = limelight;
        limelight.start();
    }

    public HardwareEntry entry() { return entry; }
}