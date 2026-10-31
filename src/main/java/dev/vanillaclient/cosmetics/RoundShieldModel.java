package dev.vanillaclient.cosmetics;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

/** Round cosmetic shield: an 18x18 plate (the round shape comes from the texture) and the vanilla handle. */
public final class RoundShieldModel extends Model<Unit> {
	public RoundShieldModel() {
		super(create(), RenderTypes::entityCutout);
	}

	private static ModelPart create() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 0).addBox(-9f, -9f, -2f, 18f, 18f, 1f), PartPose.ZERO);
		root.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(0, 20).addBox(-1f, -3f, -1f, 2f, 6f, 6f), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64).bakeRoot();
	}
}
