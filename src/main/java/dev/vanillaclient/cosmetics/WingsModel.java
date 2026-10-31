package dev.vanillaclient.cosmetics;

import dev.vanillaclient.cosmetics.WingsModel.Pose;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Cosmetic wings: two flat, two-part wings (inner wing + bendable tip) on the back.
 * Texture 32x48 units: inner wing faces at (0,0), wing tip faces at (0,16).
 */
public final class WingsModel extends Model<Pose> {
	/** Animation input: flap phase (radians, accumulated smoothly), flap strength 0..1, folding 0..1. */
	public record Pose(float phase, float flap, float fold) {}

	private final ModelPart leftWing, leftTip, rightWing, rightTip;

	public WingsModel() {
		super(create(), RenderTypes::entityCutout);
		this.leftWing = root.getChild("left_wing");
		this.leftTip = leftWing.getChild("left_tip");
		this.rightWing = root.getChild("right_wing");
		this.rightTip = rightWing.getChild("right_tip");
	}

	private static ModelPart create() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition left = root.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(0, 0).addBox(0f, -4f, 0f, 12f, 16f, 0f),
				PartPose.offset(1.5f, 2f, 2.6f));
		left.addOrReplaceChild("left_tip",
				CubeListBuilder.create().texOffs(0, 16).addBox(0f, -6f, 0f, 14f, 20f, 0f),
				PartPose.offset(12f, -2f, 0f));
		PartDefinition right = root.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-12f, -4f, 0f, 12f, 16f, 0f),
				PartPose.offset(-1.5f, 2f, 2.6f));
		right.addOrReplaceChild("right_tip",
				CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-14f, -6f, 0f, 14f, 20f, 0f),
				PartPose.offset(-12f, -2f, 0f));
		return LayerDefinition.create(mesh, 32, 48).bakeRoot();
	}

	@Override
	public void setupAnim(Pose p) {
		super.setupAnim(p);
		float amp = 0.06f + p.flap() * 0.38f;
		float s = (float) Math.sin(p.phase());
		float s2 = (float) Math.sin(p.phase() - 0.5f);
		float back = 0.5f + p.fold() * 0.55f;
		float up = -0.2f + p.fold() * 0.12f;
		float tipBend = 0.12f + p.fold() * 0.55f;
		leftWing.yRot = -(back + s * amp);
		leftWing.zRot = up - s * amp * 0.4f;
		leftTip.yRot = -(tipBend + s2 * amp * 0.6f);
		rightWing.yRot = back + s * amp;
		rightWing.zRot = -up + s * amp * 0.4f;
		rightTip.yRot = tipBend + s2 * amp * 0.6f;
	}
}
