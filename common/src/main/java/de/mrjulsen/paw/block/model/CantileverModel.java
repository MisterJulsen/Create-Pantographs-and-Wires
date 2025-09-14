package de.mrjulsen.paw.block.model;

import java.util.Optional;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.math.Axis;

import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.property.ECantileverMastConnection;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity.CantileverData;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.AbstractModel;
import de.mrjulsen.mcdragonlib.client.model.mesh.BasicMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.CornerType;
import de.mrjulsen.mcdragonlib.client.model.mesh.CubeMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.Edge;
import de.mrjulsen.mcdragonlib.client.model.mesh.EdgeType;
import de.mrjulsen.mcdragonlib.client.model.mesh.Face;
import de.mrjulsen.mcdragonlib.client.model.mesh.FaceVertex;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.Vertex;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

public class CantileverModel extends AbstractModel {
    
    @Override
    protected Mesh getMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context) {
        float width = context.has(CantileverBlockEntity.PROPERTY_WIDTH) ? context.get(CantileverBlockEntity.PROPERTY_WIDTH) : 2.5f;
        float height = context.has(CantileverBlockEntity.PROPERTY_HEIGHT) ? context.get(CantileverBlockEntity.PROPERTY_HEIGHT) : 1.5f;
        ECantileverInsulatorsPlacement insulatorPlacement = context.has(CantileverBlockEntity.PROPERTY_INSULATOR_PLACEMENT) ? context.get(CantileverBlockEntity.PROPERTY_INSULATOR_PLACEMENT) : ECantileverInsulatorsPlacement.BACK;
        ECantileverRegistrationArmType registrationArmType = context.has(CantileverBlockEntity.PROPERTY_REGISTRATION_ARM) ? context.get(CantileverBlockEntity.PROPERTY_REGISTRATION_ARM) : ECantileverRegistrationArmType.CENTER;
        float catenaryHeight = context.has(CantileverBlockEntity.PROPERTY_CATENARY_HEIGHT) ? context.get(CantileverBlockEntity.PROPERTY_CATENARY_HEIGHT) : 1;
        ECantileverMastConnection mastConnection = context.has(CantileverBlockEntity.PROPERTY_MAST_CONNECTION_TYPE) ? context.get(CantileverBlockEntity.PROPERTY_MAST_CONNECTION_TYPE) : ECantileverMastConnection.NONE;

        CantileverData[] subCantilevers = context.has(CantileverBlockEntity.PROPERTY_SUB_CANTILEVER_SETTINGS) ? context.get(CantileverBlockEntity.PROPERTY_SUB_CANTILEVER_SETTINGS) : new CantileverData[] {
            new CantileverData(0, CantileverBlockEntity.Y_POS, 0, width, height, 0, registrationArmType, catenaryHeight, 0)
        };

        BasicMesh mesh = new BasicMesh();
        float steadyArmOffset = registrationArmType.getOffset();

        if (subCantilevers != null) {
            for (CantileverData data : subCantilevers) {            
                Mesh cantilever = createCantilever(
                    random,
                    data.x(),
                    data.y(),
                    data.z(),
                    data.width(),
                    height,
                    (state.getBlock() instanceof CantileverBlock block ? block.getInsulatorType() : EInsulatorType.BROWN),
                    insulatorPlacement,
                    data.frontYOffset(),
                    data.registrationArm(),
                    data.catenaryHeight(),
                    3,
                    0.75f,
                    steadyArmOffset,
                    true
                );
                mesh.combine(false, cantilever);
            }
            
            int cantileversCount = subCantilevers.length;
            if (cantileversCount > 1 || mastConnection != ECantileverMastConnection.NONE) {
                final TextureAtlasSprite METAL = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal"));
                final CantileverData data = subCantilevers[0];
                final float w = Math.max(0.5f, data.spacing() * (cantileversCount - 1));
                CubeMesh mastBracketTop = null;
                CubeMesh mastBracketBottom = null;

                if (cantileversCount <= 1 && mastConnection == ECantileverMastConnection.HINGE) {
                    Vector3f size = new Vector3f(DragonLib.PIXEL * 3, DragonLib.PIXEL * 3, DragonLib.PIXEL * 4);
                    mastBracketTop = new CubeMesh(new Vector3f(-data.x() - DragonLib.PIXEL * 2, data.y() - DragonLib.PIXEL, 0.5f - DragonLib.PIXEL * 2), size);
                    mastBracketTop.getFaces().forEach(x -> {
                        x.setTexture(METAL);
                        x.autoUV();
                    });
                    mastBracketBottom = new CubeMesh(new Vector3f(-data.x() - DragonLib.PIXEL * 2, -data.height(), 0.5f - DragonLib.PIXEL * 2), size);
                    mastBracketBottom.getFaces().forEach(x -> {
                        x.setTexture(METAL);
                        x.autoUV();
                    });
                } else {
                    Vector3f size = new Vector3f(DragonLib.PIXEL * 1, DragonLib.PIXEL * 3, w + DragonLib.PIXEL * 6);
                    mastBracketTop = new CubeMesh(new Vector3f(-data.x(), data.y() - DragonLib.PIXEL, 0.5f - (w / 2f) - DragonLib.PIXEL * 3), size);
                    mastBracketTop.getFaces().forEach(x -> {
                        x.setTexture(METAL);
                        x.autoUV();
                    });
                    mastBracketBottom = new CubeMesh(new Vector3f(-data.x(), -data.height(), 0.5f - (w / 2f) - DragonLib.PIXEL * 3), size);
                    mastBracketBottom.getFaces().forEach(x -> {
                        x.setTexture(METAL);
                        x.autoUV();
                    });
                }
                mesh.combine(false, mastBracketTop, mastBracketBottom);
            }            
        }
        mesh.cleanUp();
        
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (facing.getAxis() == net.minecraft.core.Direction.Axis.Z) {
            facing = facing.getOpposite();
        }
        
        Vector3f center = new Vector3f(0.5f, 0, 0.5f);
		if (state.getBlock() instanceof AbstractCantileverBlock rot) {
            float hAngle = rot.getRelativeYRotation(state);
            Vec2 p = rot.getRotationPivotPoint(state);
            Vector3f pivot = new Vector3f(-p.y, 0, p.x).add(center);
            mesh.rotate(Axis.YP.rotationDegrees(hAngle), pivot);
            if (state.getValue(AbstractCantileverBlock.ROTATION) >= 3) {
                // TODO
                mesh.translate(0, 0, 1);
            }
            mesh.rotate(Axis.YP.rotationDegrees(facing.toYRot() + 90), center);
		}

        mesh.getFaces().forEach(x -> x.setCullface(null));
        return mesh;
    }

    /**
     * This is without a doubt the worst code I've ever run!
     * But it runs.
     */
    private Mesh createCantilever(RandomSource random, float xOffset, float yOffset, float zOffset, float width, float height, EInsulatorType insulatorType, ECantileverInsulatorsPlacement insulators, float frontOffset, ECantileverRegistrationArmType registrationArmType, float wireOffsetY, float registrationArmAngle, float steadyArmLength, float steadyArmOffset, boolean showBracing) {

        final TextureAtlasSprite TEXTURE = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(new ResourceLocation(PantographsAndWires.MOD_ID, "block/cantilever"));
        final TextureAtlasSprite METAL = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal"));
        final TextureAtlasSprite ANVIL = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(new ResourceLocation(PantographsAndWires.MOD_ID, "block/anvil"));

        final float bracketTubeThickness = DragonLib.PIXEL * 1f;
        final float thickness = DragonLib.PIXEL * 1f;
        final float holderThinkness = DragonLib.PIXEL * 1;
        final float steadyArmBodyHeight = DragonLib.PIXEL * 1.5f;
        final float wireAttachThickness = DragonLib.PIXEL * 2;
        final float insulatorPostDistance = DragonLib.PIXEL * 1;

        final float rawWidth = width;
        width += xOffset;

        final float insulatorHalfWidth = 0.35f;
        final float insulatorHeight = 0.2f;
        final float insulatorX = width * insulators.getPlacementOffsetFac();


        float supportZ = zOffset - (bracketTubeThickness / 2);
        float z = zOffset - (thickness / 2);
        float z2 = zOffset - (holderThinkness / 2);
        registrationArmAngle *= (registrationArmType == ECantileverRegistrationArmType.INNER ? 1 : -1);



        BasicMesh cantileverMesh = new BasicMesh();
        
        // --- BRACKET TUBE ---
        Vector3f bracketTubeOrigin = new Vector3f(rawWidth, yOffset + frontOffset, supportZ);
        CubeMesh bracketTube = new CubeMesh(bracketTubeOrigin, new Vector3f(0, bracketTubeThickness, bracketTubeThickness));

        Face bracketTubeWest = bracketTube.getFaceOnSide(Direction.WEST);
        bracketTubeWest.getEdge(EdgeType.BOTTOM).getTransformableElements().forEach(t -> t.getPos().set(-xOffset, -height, t.getPos().z()));
        float bracketTubeAngle = calcAngle(width, height);
        double bracketTubeAngleRas = Math.toRadians(bracketTubeAngle);
        double hypotenuse = bracketTubeThickness / Math.cos(bracketTubeAngleRas);
        Edge bracketTubeTopWestEdge = bracketTubeWest.getEdge(EdgeType.TOP);
        bracketTubeTopWestEdge.getTransformableElements().forEach(t -> t.getPos().set(-xOffset, -height + hypotenuse, t.getPos().z()));

        Face bracketTubeEast = bracketTube.getFaceOnSide(Direction.EAST);
        bracketTubeAngle = calcAngle(width, (float)Math.abs(bracketTubeOrigin.y() - bracketTubeTopWestEdge.center().y() + thickness));
        bracketTubeEast.rotate(Axis.ZP.rotationDegrees(bracketTubeAngle), bracketTubeEast.getEdge(EdgeType.TOP).center());
        bracketTubeEast.translate(new Vector3f(bracketTubeEast.getNormal()).mul(DragonLib.PIXEL * 1));
        final Vector3f bracketTubeNormal = bracketTube.getFaceOnSide(Direction.EAST).getNormal();

        // --- STAY TUBE ---
        Vector3f stayTubeOrigin = new Vector3f(-xOffset, yOffset, z);
        CubeMesh stayTube = new CubeMesh(stayTubeOrigin, new Vector3f(0, thickness, thickness));
        float stayTubeAngle = calcAngle(width, frontOffset);
        stayTube.rotate(Axis.ZP.rotationDegrees(stayTubeAngle), stayTubeOrigin);
        final Vector3f stayTubeNormal = stayTube.getFaceOnSide(Direction.EAST).getNormal();

        Face stayTubeEast = stayTube.getFaceOnSide(Direction.EAST);
        Vector3f[] bracketTubeCollisionPlane = bracketTube.getFaceOnSide(Direction.UP).getVertexPositionArray();
        for (FaceVertex vert : stayTubeEast.getCorners()) {
            Vertex v = vert.getVertex();
            v.setPos(intersectRayWithQuads(new Vector3f[][] { bracketTubeCollisionPlane }, v.getPos(), stayTubeNormal, false, 0).orElse(v.getPos()));
        }
        

        // --- STEADY ARM ---
        BasicMesh steadyArm = new BasicMesh();
        Vector3f registrationArmAttachPoint = new Vector3f();

        Vector3f steadyArmBaseOrigin = new Vector3f(rawWidth - DragonLib.PIXEL * 0.5f + steadyArmOffset, -wireOffsetY, zOffset - wireAttachThickness / 2f);
        CubeMesh steadyArmBase = new CubeMesh(steadyArmBaseOrigin, new Vector3f(DragonLib.PIXEL * 1, wireAttachThickness, wireAttachThickness));

        steadyArm.combine(false, steadyArmBase);
        
        if (registrationArmType != ECantileverRegistrationArmType.CENTER) {
            Vector3f steadyArmBodyOrigin = new Vector3f(steadyArmBase.getFaceOnSide(Direction.UP).getEdge(EdgeType.RIGHT).center()).add(-steadyArmLength, -steadyArmBodyHeight / 2f, -thickness / 4f);
            CubeMesh steadyArmBody = new CubeMesh(steadyArmBodyOrigin, new Vector3f(steadyArmLength, steadyArmBodyHeight, thickness / 2f));
            steadyArmBody.rotate(Axis.ZP.rotationDegrees(-5), steadyArmBase.getFaceOnSide(Direction.UP).center());
            steadyArmBody.rotate(Axis.YP.rotationDegrees(registrationArmType == ECantileverRegistrationArmType.OUTER ? 180 : 0), steadyArmBase.getFaceOnSide(Direction.UP).center());

            Vector3f steadyArmRootOrigin = new Vector3f(steadyArmBody.getFaceOnSide(Direction.WEST).getEdge(EdgeType.BOTTOM).center()).add(-holderThinkness / 2f, -DragonLib.PIXEL * 0.5f, -holderThinkness / 2f);
            CubeMesh steadyArmRoot = new CubeMesh(steadyArmRootOrigin, new Vector3f(holderThinkness, DragonLib.PIXEL * 3.5f, holderThinkness));

            steadyArm.combine(false, steadyArmBody, steadyArmRoot);
            registrationArmAttachPoint = steadyArmRoot.getFaceOnSide(Direction.UP).center();
            
            steadyArmBody.getFaces().forEach(x -> {
                //x.setColor(Color.MAGENTA);
                x.setTexture(METAL);
                x.autoUV(CornerType.TOP_LEFT, 0.5f);
            });
            steadyArmRoot.getFaces().forEach(x -> {
                //x.setColor(Color.CYAN);
                x.setTexture(METAL);
                x.autoUV(CornerType.TOP_LEFT, 0.5f);
            });
        } else {
            registrationArmAttachPoint = steadyArmBase.getFaceOnSide(Direction.UP).getEdge(EdgeType.RIGHT).center();
        }

        // --- REGISTRATION ARM ---
        Vector3f registrationArmOrigin = new Vector3f(registrationArmAttachPoint).add(0, -thickness / 2, -thickness / 2f);
        CubeMesh registrationArm = new CubeMesh(registrationArmOrigin, new Vector3f(thickness, thickness, thickness));
        registrationArm.rotate(Axis.ZP.rotationDegrees(registrationArmAngle), registrationArmOrigin);
        final Vector3f registrationArmNormal = registrationArm.getFaceOnSide(Direction.EAST).getNormal();
        float extendedLength = switch (registrationArmType) {
            case INNER -> 1.1f;
            case OUTER -> 0.1f;
            default -> 0;
        };
        registrationArm.translate(new Vector3f(registrationArmNormal).mul(extendedLength));

        Face registrationArmWest = registrationArm.getFaceOnSide(Direction.WEST);
        Vector3f registrationArmWestNormal = registrationArmWest.getNormal();
        Vector3f[] registrationArmCollisionPlane = bracketTube.getFaceOnSide(Direction.DOWN).getVertexPositionArray();
        for (FaceVertex vert : registrationArmWest.getCorners()) {
            Vertex v = vert.getVertex();
            v.setPos(intersectRayWithQuads(new Vector3f[][] { registrationArmCollisionPlane, new Vector3f[] { new Vector3f(-xOffset, 0, 0), new Vector3f(-xOffset, 1, 0), new Vector3f(-xOffset, 1, 1), new Vector3f(-xOffset, 0, 1) } }, v.getPos(), registrationArmWestNormal, false, 0).orElse(v.getPos()));
        }

        // ---            ---
        // --- INSULATORS ---
        // ---            ---
        Vector3f registrationArmConnectPoint = registrationArm.getFaceOnSide(Direction.WEST).center();

        // --- STAY TUBE INSULATOR ---
        float registrationArmYDistToBracketTube = Math.max(0, bracketTubeWest.center().y() - registrationArmWest.center().y());
        float stayTubeLength = stayTube.getFaceOnSide(Direction.EAST).center().distance(stayTube.getFaceOnSide(Direction.WEST).center());
        float stayTubeInsulatorMinDistance = calcAdjacent(bracketTubeAngle - stayTubeAngle, (float)lerp(insulatorHeight, insulatorHalfWidth, bracketTubeAngle / 90f)) + insulatorHalfWidth;
        float stayTubeInsulatorMaxDistance = stayTubeLength - calcHypotenuseFromAdjacent(stayTubeAngle, (float)lerp(insulatorHalfWidth, insulatorHeight, stayTubeAngle / 90f) + insulatorPostDistance);
        float stayTubeInsulatorDistance = calcHypotenuseFromAdjacent(stayTubeAngle, width - insulatorX + insulatorHalfWidth);

        if (stayTubeInsulatorMaxDistance > stayTubeInsulatorMinDistance) {
            Vector3f stayTubeInsulatorPos = new Vector3f(stayTubeNormal).negate().mul(MathUtils.clamp(stayTubeInsulatorDistance, stayTubeInsulatorMinDistance, stayTubeInsulatorMaxDistance));
            BasicMesh stayTubeInsulatorMesh = BasicMesh.fromBlock(insulatorType.getInsulatorBlock().defaultBlockState(), random);
            stayTubeInsulatorMesh.rotate(Axis.ZP.rotationDegrees(-90 + stayTubeAngle), stayTubeInsulatorMesh.center());
            stayTubeInsulatorMesh.centerTo(stayTube.getFaceOnSide(Direction.EAST).center());
            stayTubeInsulatorMesh.translate(stayTubeInsulatorPos);

            cantileverMesh.combine(false, stayTubeInsulatorMesh);
        }
        
        // --- REGISTRATION ARM INSULATOR (Optional) ---
        Vector3f registrationArmEastPos = registrationArm.getFaceOnSide(Direction.EAST).center();
        float registrationArmLengthOffset = calcHypotenuseFromAdjacent(registrationArmAngle, registrationArmEastPos.x() - Math.min(steadyArmBaseOrigin.x(), registrationArmAttachPoint.x())) + (registrationArmType == ECantileverRegistrationArmType.CENTER ? insulatorHalfWidth : 0);
        float registrationArmLength = registrationArmEastPos.distance(registrationArm.getFaceOnSide(Direction.WEST).center()) - registrationArmLengthOffset - insulatorPostDistance;
        float registrationArmInsulatorMaxDistance = registrationArmLength - calcAdjacent(bracketTubeAngle - registrationArmAngle, Math.max(0, insulatorHeight - registrationArmYDistToBracketTube)) - insulatorHalfWidth;
        float registrationArmInsulatorMinDistance = insulatorHalfWidth;
        float registrationArmInsulatorDistance = calcHypotenuseFromAdjacent(registrationArmAngle, Math.max(width, steadyArmBaseOrigin.x()) - insulatorX);
        
        boolean registrationArmPossible = registrationArmInsulatorMaxDistance > registrationArmInsulatorMinDistance;
        Vector3f registrationArmInsulatorPos = new Vector3f(registrationArmNormal).negate().mul(registrationArmLengthOffset + MathUtils.clamp(registrationArmInsulatorDistance, registrationArmInsulatorMinDistance, registrationArmInsulatorMaxDistance));
        BasicMesh registrationArmInsulatorMesh = BasicMesh.fromBlock(insulatorType.getInsulatorBlock().defaultBlockState(), random);
        registrationArmInsulatorMesh.rotate(Axis.ZP.rotationDegrees(-90 + registrationArmAngle), registrationArmInsulatorMesh.center());
        registrationArmInsulatorMesh.centerTo(registrationArm.getFaceOnSide(Direction.EAST).center());
        registrationArmInsulatorMesh.translate(registrationArmInsulatorPos);
        

        // --- BRACKET TUBE INSULATOR ---
        Vector3f bracketTubeEastPos = bracketTube.getFaceOnSide(Direction.EAST).center();
        float bracketTubeStartX = bracketTubeEastPos.x() - stayTubeEast.center().x();
        float bracketTubeLengthOffset = calcHypotenuseFromAdjacent(registrationArmAngle, bracketTubeStartX);
        float bracketTubeLength = bracketTubeEastPos.distance(bracketTube.getFaceOnSide(Direction.WEST).center()) - bracketTubeLengthOffset;
        
        float bracketTubeInsulatorMinDistance = calcAdjacent(bracketTubeAngle - stayTubeAngle, (float)lerp(insulatorHeight, insulatorHalfWidth, bracketTubeAngle / 90f)) + insulatorHalfWidth;
        float bracketTubeInsulatorMaxDistance = bracketTubeLength - calcHypotenuseFromAdjacent(bracketTubeAngle, Math.max(0, (float)lerp(insulatorHalfWidth, insulatorHeight, bracketTubeAngle / 90f) - registrationArmYDistToBracketTube) + insulatorPostDistance);

        float bracketTubeInsulatorRDistance = calcHypotenuseFromAdjacent(bracketTubeAngle, bracketTube.getFaceOnSide(Direction.EAST).getEdge(EdgeType.BOTTOM).center().x() - registrationArmConnectPoint.x()) - bracketTubeLengthOffset;
        float bracketTubeInsulatorLDistance = bracketTubeInsulatorRDistance + insulatorHalfWidth + calcHypotenuseFromOpposite(bracketTubeAngle - registrationArmAngle, thickness / 2f);
        float bracketTubeInsulatorUDistance = bracketTubeInsulatorRDistance - calcAdjacent(bracketTubeAngle - registrationArmAngle, Math.max(insulatorPostDistance, insulatorHeight - registrationArmYDistToBracketTube)) - insulatorHalfWidth;
        
        float bracketTubeInsulatorDistance = calcHypotenuseFromAdjacent(bracketTubeAngle, width - insulatorX + insulatorHalfWidth);

        boolean upperPossible = bracketTubeInsulatorMinDistance < bracketTubeInsulatorUDistance && registrationArmPossible;
        boolean lowerPossible = bracketTubeInsulatorLDistance < bracketTubeInsulatorMaxDistance;
        float result = 0;
        if (upperPossible && lowerPossible && bracketTubeInsulatorDistance > bracketTubeInsulatorUDistance && bracketTubeInsulatorDistance < bracketTubeInsulatorLDistance) {
            result = closestEdge(bracketTubeInsulatorDistance, bracketTubeInsulatorUDistance, bracketTubeInsulatorLDistance);
        } else if (upperPossible || lowerPossible) {
            float lowerBound = lowerPossible ? bracketTubeInsulatorMaxDistance : Math.min(bracketTubeInsulatorMaxDistance, bracketTubeInsulatorUDistance);
            float upperBound = upperPossible ? bracketTubeInsulatorMinDistance : Math.max(bracketTubeInsulatorMinDistance, bracketTubeInsulatorLDistance);
            result = MathUtils.clamp(bracketTubeInsulatorDistance, upperBound, lowerBound);
        }
        boolean bracketTubeInsulatorPossible = result > 0;

        if (bracketTubeInsulatorPossible) {            
            Vector3f bracketTubeInsulatorPos = new Vector3f(bracketTubeNormal).negate().mul(bracketTubeLengthOffset + result);
            BasicMesh bracketTubeInsulatorMesh = BasicMesh.fromBlock(insulatorType.getInsulatorBlock().defaultBlockState(), random);
            bracketTubeInsulatorMesh.rotate(Axis.ZP.rotationDegrees(-90 + bracketTubeAngle), bracketTubeInsulatorMesh.center());
            bracketTubeInsulatorMesh.centerTo(bracketTube.getFaceOnSide(Direction.EAST).center());
            bracketTubeInsulatorMesh.translate(bracketTubeInsulatorPos);
            cantileverMesh.combine(false, bracketTubeInsulatorMesh);
        }

        if (registrationArmPossible && result < bracketTubeInsulatorLDistance) {
            cantileverMesh.combine(false, registrationArmInsulatorMesh);
        }

        

        // --- BRACING ---
        boolean bracigBehindInsulator = width * insulators.getPlacementOffsetFac() > insulatorHalfWidth + insulatorPostDistance + thickness * 2;
        Vector3f bracingOrigin = new Vector3f((bracigBehindInsulator ? 0 : insulatorHalfWidth * 2) - xOffset + insulatorPostDistance, yOffset, z2);
        CubeMesh bracing = new CubeMesh(bracingOrigin, new Vector3f(0, holderThinkness, holderThinkness));
        float bracingAngle = calcAngle(registrationArmWest.getEdge(EdgeType.TOP).center().x() - bracingOrigin.x() - holderThinkness, registrationArmWest.getEdge(EdgeType.TOP).center().y() - bracingOrigin.y() - holderThinkness);
        boolean backPicked = (lowerPossible && !upperPossible) || insulators == ECantileverInsulatorsPlacement.BACK;

        if (showBracing && Math.abs(bracingAngle) < 90 && bracigBehindInsulator != backPicked) {
            bracing.rotate(Axis.ZP.rotationDegrees(bracingAngle), bracingOrigin);

            Face bracingWest = bracing.getFaceOnSide(Direction.WEST);
            Vector3f bracingWestNormal = bracingWest.getNormal();
            bracingWest.translate(new Vector3f(bracingWestNormal).negate());
            Vector3f[] bracingCollisionPlaneDown = stayTube.getFaceOnSide(Direction.DOWN).getVertexPositionArray();
            for (FaceVertex vert : bracingWest.getCorners()) {
                Vertex v = vert.getVertex();
                v.setPos(intersectRayWithQuads(new Vector3f[][] { bracingCollisionPlaneDown }, v.getPos(), bracingWestNormal, false, 0).orElse(v.getPos()));
            }

            Face bracingEast = bracing.getFaceOnSide(Direction.EAST);
            Vector3f bracingEastNormal = bracingEast.getNormal();
            Vector3f[] bracingCollisionPlaneUp = bracketTube.getFaceOnSide(Direction.UP).getVertexPositionArray();
            for (FaceVertex vert : bracingEast.getCorners()) {
                Vertex v = vert.getVertex();
                v.setPos(intersectRayWithQuads(new Vector3f[][] { bracingCollisionPlaneUp }, v.getPos(), bracingEastNormal, false, 0).orElse(v.getPos()));
            }
            cantileverMesh.combine(false, bracing);
        }
        cantileverMesh.combine(false, stayTube, bracketTube, registrationArm, steadyArm);

        // TEXTURING
        
        bracketTube.getFaces().forEach(x -> {
            //x.setColor(Color.RED);
            x.setTexture(TEXTURE);
            x.autoUV(CornerType.TOP_LEFT, 0.5f);
        });
        stayTube.getFaces().forEach(x -> {
            //x.setColor(Color.BLUE);
            x.setTexture(TEXTURE);
            x.autoUV(CornerType.TOP_LEFT, 0.5f);
        });
        steadyArmBase.getFaces().forEach(x -> {
            //x.setColor(Color.WHITE);
            x.setTexture(ANVIL);
            x.autoUV(CornerType.TOP_LEFT, 0.5f);
        });
        registrationArm.getFaces().forEach(x -> {
            //x.setColor(Color.GREEN);
            x.setTexture(TEXTURE);
            x.autoUV(CornerType.TOP_LEFT, 0.5f);
        });
        bracing.getFaces().forEach(x -> {
            //x.setColor(Color.YELLOW);
            x.setTexture(TEXTURE);
            x.autoUV(CornerType.TOP_LEFT, 0.5f);
        });

        cantileverMesh.cleanUp();
        return cantileverMesh;
    }

    public static float closestEdge(float input, float min, float max) {
        float distToMin = Math.abs(input - min);
        float distToMax = Math.abs(input - max);
        return distToMin <= distToMax ? min : max;
    }

    private static float calcAngle(float width, float height) {
        double angleRad = Math.atan(height / width);
        double angleDeg = Math.toDegrees(angleRad);
        return (float)angleDeg;
    }

    private static float calcHypotenuseFromAdjacent(float angle, float adjacent) {
        double rad = Math.toRadians(angle);
        float result = adjacent / (float)Math.cos(rad);
        return result;
    }

    private static float calcHypotenuseFromOpposite(float angle, float opposite) {
        double rad = Math.toRadians(angle);
        float result = opposite / (float)Math.sin(rad);
        return result;
    }


    private static float calcAdjacent(float angle, float opposite) {
        double rad = Math.toRadians(angle);
        float result = (float)(opposite / Math.tan(rad));
        return result;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double mapAngleToValue(double angleDegrees) {
        if (angleDegrees < 0) angleDegrees = 0;
        if (angleDegrees > 90) angleDegrees = 90;

        double t = angleDegrees / 90.0;
        return lerp(0.3, 0.1, t);
    }


    public static Optional<Vector3f> intersectRayWithQuads(Vector3f[][] quads, Vector3f origin, Vector3f direction, boolean boundCheck, float maxDistance) {
        Vector3f closestIntersection = null;
        float closestT = Float.MAX_VALUE;

        for (Vector3f[] quad : quads) {
            if (quad == null || quad.length != 4) {
                throw new IllegalArgumentException("Each quad must have 4 vertices.");
            }

            Vector3f edge1 = new Vector3f();
            quad[1].sub(quad[0], edge1);
            Vector3f edge2 = new Vector3f();
            quad[3].sub(quad[0], edge2);

            Vector3f normal = new Vector3f();
            edge1.cross(edge2, normal).normalize();

            float denom = normal.dot(direction);
            if (Math.abs(denom) < 1e-6f) {
                continue;
            }

            Vector3f diff = new Vector3f();
            quad[0].sub(origin, diff);
            float t = diff.dot(normal) / denom;

            if (t < 0 || (maxDistance > 0 && t > maxDistance)) {
                continue;
            }

            Vector3f intersection = new Vector3f(direction).mul(t).add(origin);

            if (boundCheck) {
                if (!(pointInTriangle(intersection, quad[0], quad[1], quad[2]) ||
                    pointInTriangle(intersection, quad[0], quad[2], quad[3]))) {
                    continue;
                }
            }

            if (t < closestT) {
                closestT = t;
                closestIntersection = intersection;
            }
        }

        return Optional.ofNullable(closestIntersection);
    }


    private static boolean pointInTriangle(Vector3f p, Vector3f a, Vector3f b, Vector3f c) {
        Vector3f v0 = new Vector3f();
        Vector3f v1 = new Vector3f();
        Vector3f v2 = new Vector3f();

        c.sub(a, v0);
        b.sub(a, v1);
        p.sub(a, v2);

        float d00 = v0.dot(v0);
        float d01 = v0.dot(v1);
        float d11 = v1.dot(v1);
        float d20 = v2.dot(v0);
        float d21 = v2.dot(v1);

        float denom = d00 * d11 - d01 * d01;
        if (denom == 0.0f) return false;

        float v = (d11 * d20 - d01 * d21) / denom;
        float w = (d00 * d21 - d01 * d20) / denom;
        float u = 1.0f - v - w;

        return u >= 0 && v >= 0 && w >= 0;
    }

    public static Vector3f getDirectionFromAngle(net.minecraft.core.Direction.Axis axis, float degrees) {
        Quaternionf rotation = new Quaternionf();
        float radians = (float)Math.toRadians(degrees);

        switch (axis) {
            case X -> rotation.rotateX(radians);
            case Y -> rotation.rotateY(radians);
            case Z -> rotation.rotateZ(radians);
        }

        Vector3f normal = new Vector3f(0, 0, -1);
        rotation.transform(normal);
        return normal.normalize();
    }

    
}
