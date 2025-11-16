package de.mrjulsen.paw.fabric.client.model.loaders;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.fabricators_of_create.porting_lib.core.PortingLib;
import net.createmod.catnip.math.VecHelper;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;

import io.github.fabricators_of_create.porting_lib.models.UnbakedGeometryHelper;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.models.obj.ObjBakedModel;
import io.github.fabricators_of_create.porting_lib.models.obj.ObjMaterialLibrary;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * A model loaded from an OBJ file.
 * <p>
 * Supports positions, texture coordinates, normals and colors. The {@link ObjMaterialLibrary material library}
 * has support for numerous features, including support for {@link ResourceLocation} textures (non-standard).
 */
public class MultipartObjModel implements IUnbakedGeometry<MultipartObjModel>, UnbakedModel {
	public static final boolean ENABLED;
	private static final Renderer renderer;
	private static final RenderMaterial diffuseMaterial;
	private static final RenderMaterial defaultMaterial;

	static {
		renderer = RendererAccess.INSTANCE.getRenderer();
		ENABLED = renderer != null;
		if (ENABLED) {
			diffuseMaterial = renderer.materialFinder().disableDiffuse(true).find();
			defaultMaterial = renderer.materialById(RenderMaterial.MATERIAL_STANDARD);
		} else {
			PortingLib.LOGGER.error("The Fabric Rendering API is not available. If you have Sodium, install Indium!");
			diffuseMaterial = defaultMaterial = null;
		}
	}

	private static final Vector4f COLOR_WHITE = new Vector4f(1, 1, 1, 1);
	private static final Vec2[] DEFAULT_COORDS = {
			new Vec2(0, 0),
			new Vec2(0, 1),
			new Vec2(1, 1),
			new Vec2(1, 0),
	};

	final Map<String, ModelGroup> parts = Maps.newLinkedHashMap();
	private final Set<String> rootComponentNames = Collections.unmodifiableSet(parts.keySet());
	private Set<String> allComponentNames;

	final List<Vector3f> positions = Lists.newArrayList();
	final List<Vec2> texCoords = Lists.newArrayList();
	final List<Vector3f> normals = Lists.newArrayList();
	final List<Vector4f> colors = Lists.newArrayList();

	public final boolean automaticCulling;
	public final boolean shadeQuads;
	public final boolean flipV;
	public final boolean emissiveAmbient;
	@Nullable
	public final String mtlOverride;

	public final ResourceLocation modelLocation;

	public MultipartObjModel(ModelSettings settings) {
		this.modelLocation = settings.modelLocation;
		this.automaticCulling = settings.automaticCulling;
		this.shadeQuads = settings.shadeQuads;
		this.flipV = settings.flipV;
		this.emissiveAmbient = settings.emissiveAmbient;
		this.mtlOverride = settings.mtlOverride;
	}

    public void addPart(String name, ModelGroup part) {
        parts.computeIfAbsent(name, a -> part);
    }
    
    public Collection<? extends ModelObject> getParts() {
        return parts.values();
    }

	public Set<String> getRootComponentNames() {
		return rootComponentNames;
	}

	/**
	 * Bake from custom block model geometry
	 */
	@Override
	public BakedModel bake(BlockModel owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
						   ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation, boolean isGui3d) {
		ImmutableList<Mesh> meshes = bakeMeshes(owner, baker, spriteGetter, modelTransform);
		TextureAtlasSprite particle = spriteGetter.apply(owner.getMaterial("particle"));
		return new ObjBakedModel(
				meshes, owner.hasAmbientOcclusion(), isGui3d, owner.getGuiLight().lightLikeBlock(),
				false, owner.getTransforms(), overrides, particle
		);
	}

	/**
	 * Bake from a standalone model
	 */
	@Nullable
	@Override
	public BakedModel bake(@NotNull ModelBaker baker, @NotNull Function<Material, TextureAtlasSprite> spriteGetter,
						   @NotNull ModelState modelTransform, @NotNull ResourceLocation modelLocation) {
		ImmutableList<Mesh> meshes = bakeMeshes(null, baker, spriteGetter, modelTransform);
		TextureAtlasSprite particle = spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation()));
		return new ObjBakedModel(
				meshes, false, false, false, false,
				ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY, particle
		);
	}

	private ImmutableList<Mesh> bakeMeshes(BlockModel owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform) {
		ImmutableList.Builder<Mesh> bakedMeshes = new ImmutableList.Builder<>();
		parts.values().forEach(part -> {
			MeshBuilder meshBuilder = renderer.meshBuilder();
			part.buildMeshes(owner, meshBuilder, baker, spriteGetter, modelTransform, modelLocation);
			bakedMeshes.add(meshBuilder.build());
		});
		return bakedMeshes.build();
	}

	@Override
	public Set<String> getConfigurableComponentNames() {
		if (allComponentNames != null) {
			return allComponentNames;
		}
		var names = new HashSet<String>();
		for (var group : parts.values()) {
			group.addNamesRecursively(names);
		}
		return allComponentNames = Collections.unmodifiableSet(names);
	}

	private void makeQuad(SubModelSettings subSettings, MeshBuilder builder, int[][] indices, int tintIndex, Vector4f colorTint, Vector4f ambientColor, TextureAtlasSprite texture, Transformation transform) {
		boolean needsNormalRecalculation = false;
		for (int[] ints : indices) {
			needsNormalRecalculation |= ints.length < 3;
		}
		Vector3f faceNormal = new Vector3f();
		if (needsNormalRecalculation) {
			Vector3f a = positions.get(indices[0][0]);
			Vector3f ab = positions.get(indices[1][0]);
			Vector3f ac = positions.get(indices[2][0]);
			Vector3f abs = new Vector3f(ab);
			abs.sub(a);
			Vector3f acs = new Vector3f(ac);
			acs.sub(a);
			abs.cross(acs);
			abs.normalize();
			faceNormal = abs;
		}

		var quadBaker = builder.getEmitter();

		quadBaker.spriteBake(texture, MutableQuadView.BAKE_ROTATE_NONE);
		quadBaker.colorIndex(tintIndex);

		int uv2 = 0;
		if (emissiveAmbient) {
			int fakeLight = (int) ((ambientColor.x() + ambientColor.y() + ambientColor.z()) * 15 / 3.0f);
			uv2 = LightTexture.pack(fakeLight, fakeLight);
			quadBaker.material((fakeLight == 0 && shadeQuads) ? defaultMaterial : diffuseMaterial);
		} else {
			quadBaker.material(shadeQuads ? defaultMaterial : diffuseMaterial);
		}

		boolean hasTransform = !transform.isIdentity();
		// The incoming transform is referenced on the center of the block, but our coords are referenced on the corner
		Transformation transformation = hasTransform ? transform.blockCenterToCorner() : transform;

        if (subSettings != null) {
            Vector3f eulerRadians = transformation.getLeftRotation().getEulerAnglesYXZ(new Vector3f());
            Vector3f oldRot = new Vector3f(
                (float) Math.toDegrees(eulerRadians.x),
                (float) Math.toDegrees(eulerRadians.y),
                (float) Math.toDegrees(eulerRadians.z)
            );
            Vector3f rot = new Vector3f(subSettings.rotX(), subSettings.rotY(), subSettings.rotZ());
            Vec3 offset = new Vec3(subSettings.x() / 16f, subSettings.y() / 16f, subSettings.z() / 16f);
            offset = VecHelper.rotateCentered(offset, oldRot.x(), net.minecraft.core.Direction.Axis.X);
            offset = VecHelper.rotateCentered(offset, oldRot.y(), net.minecraft.core.Direction.Axis.Y);
            offset = VecHelper.rotateCentered(offset, oldRot.z(), net.minecraft.core.Direction.Axis.Z);
            Quaternionf q = Axis.YP.rotationDegrees((float)(rot.y() + oldRot.y()));
            q.mul(Axis.XP.rotationDegrees((float)(rot.x() + oldRot.x())));
            q.mul(Axis.ZP.rotationDegrees((float)(rot.z() + oldRot.z())));
            transformation = new Transformation(new Vector3f((float)offset.x(), (float)offset.y(), (float)offset.z()), q, transformation.getScale(), transformation.getRightRotation());
            hasTransform = true;
        }

		Vector4f[] pos = new Vector4f[4];
		Vector3f[] norm = new Vector3f[4];

		for (int i = 0; i < 4; i++) {
			int[] index = indices[Math.min(i, indices.length - 1)];
			Vector4f position = new Vector4f(positions.get(index[0]), 1);
			Vec2 texCoord = index.length >= 2 && texCoords.size() > 0 ? texCoords.get(index[1]) : DEFAULT_COORDS[i];
			Vector3f norm0 = !needsNormalRecalculation && index.length >= 3 && normals.size() > 0 ? normals.get(index[2]) : faceNormal;
			Vector3f normal = norm0;
			Vector4f color = index.length >= 4 && colors.size() > 0 ? colors.get(index[3]) : COLOR_WHITE;
			if (hasTransform) {
				normal = new Vector3f(norm0);
				transformation.transformPosition(position);
				transformation.transformNormal(normal);
			}
			Vector4f tintedColor = new Vector4f(
					color.x() * colorTint.x(),
					color.y() * colorTint.y(),
					color.z() * colorTint.z(),
					color.w() * colorTint.w());
			quadBaker.pos(i, position.x(), position.y(), position.z());
			int spriteColor = encodeQuadColor(tintedColor);
			quadBaker.color(spriteColor, spriteColor, spriteColor, spriteColor);
			quadBaker.uv(i,
					texture.getU(texCoord.x * 16),
					texture.getV((flipV ? 1 - texCoord.y : texCoord.y) * 16)
			);
			quadBaker.lightmap(i, uv2);
			quadBaker.normal(i, normal);
			if (i == 0) {
				quadBaker.nominalFace(Direction.getNearest(normal.x(), normal.y(), normal.z()));
			}

			pos[i] = position;
			norm[i] = normal;
		}

		Direction cull = null;
		if (automaticCulling) {
			if (Mth.equal(pos[0].x(), 0) && // vertex.position.x
					Mth.equal(pos[1].x(), 0) &&
					Mth.equal(pos[2].x(), 0) &&
					Mth.equal(pos[3].x(), 0) &&
					norm[0].x() < 0) // vertex.normal.x
			{
				cull = Direction.WEST;
			} else if (Mth.equal(pos[0].x(), 1) && // vertex.position.x
					Mth.equal(pos[1].x(), 1) &&
					Mth.equal(pos[2].x(), 1) &&
					Mth.equal(pos[3].x(), 1) &&
					norm[0].x() > 0) // vertex.normal.x
			{
				cull = Direction.EAST;
			} else if (Mth.equal(pos[0].z(), 0) && // vertex.position.z
					Mth.equal(pos[1].z(), 0) &&
					Mth.equal(pos[2].z(), 0) &&
					Mth.equal(pos[3].z(), 0) &&
					norm[0].z() < 0) // vertex.normal.z
			{
				cull = Direction.NORTH; // can never remember
			} else if (Mth.equal(pos[0].z(), 1) && // vertex.position.z
					Mth.equal(pos[1].z(), 1) &&
					Mth.equal(pos[2].z(), 1) &&
					Mth.equal(pos[3].z(), 1) &&
					norm[0].z() > 0) // vertex.normal.z
			{
				cull = Direction.SOUTH;
			} else if (Mth.equal(pos[0].y(), 0) && // vertex.position.y
					Mth.equal(pos[1].y(), 0) &&
					Mth.equal(pos[2].y(), 0) &&
					Mth.equal(pos[3].y(), 0) &&
					norm[0].y() < 0) // vertex.normal.z
			{
				cull = Direction.DOWN; // can never remember
			} else if (Mth.equal(pos[0].y(), 1) && // vertex.position.y
					Mth.equal(pos[1].y(), 1) &&
					Mth.equal(pos[2].y(), 1) &&
					Mth.equal(pos[3].y(), 1) &&
					norm[0].y() > 0) // vertex.normal.y
			{
				cull = Direction.UP;
			}
		}

		quadBaker.cullFace(cull);
		quadBaker.emit();
	}

	// Honestly I don't know what the fuck this is doing... or if it will work across different renderer implementations
	private int encodeQuadColor(Vector4f colorTint) {
		int r = (int) (colorTint.x() * 255.0F);
		int g = (int) (colorTint.y() * 255.0F);
		int b = (int) (colorTint.z() * 255.0F);
		int a = (int) (colorTint.w() * 255.0F);

		return ((a & 0xFF) << 24) |
				((b & 0xFF) << 16) |
				((g & 0xFF) << 8) |
				(r & 0xFF);
	}

	@Override
	@NotNull
	public Collection<ResourceLocation> getDependencies() {
		return List.of();
	}

	@Override
	public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> function) {
	}

	public class ModelObject {
		public final String name;
        final SubModelSettings settings;

		List<ModelMesh> meshes = Lists.newArrayList();

		ModelObject(String name, SubModelSettings settings) {
			this.name = name;
            this.settings = settings;
		}

        public ModelObject copy(SubModelSettings settings) {
            ModelObject m = new ModelObject(name, this.settings.combineTransform(settings));
            m.meshes.addAll(meshes.stream().map(x -> x.copy(this.settings.combineTransform(settings))).toList());
            return m;
        }

		public String name() {
			return name;
		}

		public void buildMeshes(@Nullable BlockModel owner, MeshBuilder meshBuilder, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ResourceLocation modelLocation) {
			for (ModelMesh mesh : meshes) {
				mesh.buildMesh(owner, meshBuilder, spriteGetter, modelTransform);
			}
		}

		public Collection<Material> getTextures(BlockModel owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<com.mojang.datafixers.util.Pair<String, String>> missingTextureErrors) {
			return meshes.stream()
					.flatMap(mesh -> mesh.mat != null
							? Stream.of(UnbakedGeometryHelper.resolveDirtyMaterial(mesh.mat.diffuseColorMap, owner))
							: Stream.of())
					.collect(Collectors.toSet());
		}

		protected void addNamesRecursively(Set<String> names) {
			names.add(name());
		}
	}

	public class ModelGroup extends ModelObject {
		final Map<String, ModelObject> parts = Maps.newLinkedHashMap();

		ModelGroup(String name, SubModelSettings settings) {
			super(name, settings);
		}

        public Collection<? extends ModelObject> getParts() {
            return parts.values();
        }

        public ModelGroup copy(SubModelSettings settings, String name) {
            ModelGroup m = new ModelGroup(name, this.settings.combineTransform(settings));
            m.parts.putAll(parts.entrySet().stream().collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue().copy(a.getValue().settings.combineTransform(settings)))));
            m.meshes.addAll(meshes.stream().map(x -> x.copy(settings.combineTransform(x.subSettings))).toList());
            return m;
        }

		@Override
		public void buildMeshes(BlockModel owner, MeshBuilder meshBuilder, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ResourceLocation modelLocation) {
			super.buildMeshes(owner, meshBuilder, baker, spriteGetter, modelTransform, modelLocation);

			parts.values().stream().filter(part -> owner.isComponentVisible(part.name(), true))
					.forEach(part -> part.buildMeshes(owner, meshBuilder, baker, spriteGetter, modelTransform, modelLocation));
		}

		@Override
		public Collection<Material> getTextures(BlockModel owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<com.mojang.datafixers.util.Pair<String, String>> missingTextureErrors) {
			Set<Material> combined = Sets.newHashSet();
			combined.addAll(super.getTextures(owner, modelGetter, missingTextureErrors));
			for (ModelObject part : parts.values()) {
				combined.addAll(part.getTextures(owner, modelGetter, missingTextureErrors));
			}
			return combined;
		}

		@Override
		protected void addNamesRecursively(Set<String> names) {
			super.addNamesRecursively(names);
			for (ModelObject object : parts.values()) {
				object.addNamesRecursively(names);
			}
		}
	}

	class ModelMesh {
		@Nullable
		public ObjMaterialLibrary.Material mat;
		@Nullable
		public String smoothingGroup;
		public final List<int[][]> faces = Lists.newArrayList();
        public SubModelSettings subSettings;

		public ModelMesh(SubModelSettings subSettings, @Nullable ObjMaterialLibrary.Material currentMat, @Nullable String currentSmoothingGroup) {
			this.mat = currentMat;
			this.smoothingGroup = currentSmoothingGroup;
			this.subSettings = subSettings;
		}

        public ModelMesh copy(SubModelSettings subSettings) {
            ModelMesh m = new ModelMesh(subSettings, mat, smoothingGroup);
            m.faces.addAll(faces);
            return m;
        }

		public void buildMesh(@Nullable BlockModel owner, MeshBuilder meshBuilder, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform) {
			if (mat == null) {
				return;
			}
			TextureAtlasSprite texture = spriteGetter.apply(UnbakedGeometryHelper.resolveDirtyMaterial(mat.diffuseColorMap, owner));
			int tintIndex = mat.diffuseTintIndex;
			Vector4f colorTint = mat.diffuseColor;

			var rootTransform = owner != null ? owner.getRootTransform() : Transformation.identity();
			var transform = rootTransform.isIdentity() ? modelTransform.getRotation() : modelTransform.getRotation().compose(rootTransform);
			for (int[][] face : faces) {
				makeQuad(subSettings, meshBuilder, face, tintIndex, colorTint, mat.ambientColor, texture, transform);
			}
		}
	}

    public record ModelSettings(@NotNull ResourceLocation modelLocation,
            boolean automaticCulling, boolean shadeQuads, boolean flipV,
            boolean emissiveAmbient, @Nullable String mtlOverride,
            List<SubModelSettings> subSettings) {
    }	

    public static class SubModelSettings {
        String model;
        float[] offset = new float[3];
        float[] rotation = new float[3];
        boolean centered = false;
        boolean inheritable = true;

        public SubModelSettings() {}

        public SubModelSettings(String model, float[] offset, float[] rotation, boolean centered, boolean inheritable) {
            this.model = model;
            this.offset = offset;
            this.rotation = rotation;
            this.centered = centered;
            this.inheritable = inheritable;
        }

        public String model() {
            return model;
        }

        public float x() {
            return offset[0] - (centered ? 8 : 0);
        }

        public float y() {
            return offset[1] - (centered ? 8 : 0);
        }

        public float z() {
            return offset[2] - (centered ? 8 : 0);
        }

        public float rotX() {
            return rotation[0];
        }

        public float rotY() {
            return rotation[1];
        }

        public float rotZ() {
            return rotation[2];
        }

        public boolean centered() {
            return centered;
        }

        public boolean isJson() {
            return model.endsWith(".json");
        }

        public boolean inheritable() {
            return inheritable;
        }

        public SubModelSettings combineTransform(@Nullable SubModelSettings other) {
            if (other == null) {
                return new SubModelSettings(
                    model(),
                    new float[] {
                        offset[0],
                        offset[1],
                        offset[2]
                    },
                    new float[] {
                        rotation[0],
                        rotation[1],
                        rotation[2]
                    },
                    centered(),
                    other == null ? inheritable() : other.inheritable()
                );
            }
            return new SubModelSettings(
                model(),
                new float[] {
                    offset[0] + other.offset[0],
                    offset[1] + other.offset[1],
                    offset[2] + other.offset[2]
                },
                new float[] {
                    rotation[0] + other.rotation[0],
                    rotation[1] + other.rotation[1],
                    rotation[2] + other.rotation[2]
                },
                centered(),
                other == null ? inheritable() : other.inheritable()
            );
        }
    }
}
