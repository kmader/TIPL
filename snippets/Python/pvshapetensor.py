import os, sys

try:
    paraview.simple
except:
    from paraview.simple import *
paraview.simple._DisableFirstRenderCameraReset()
LoadPlugin(
    "/Applications/paraview.app/Contents/Plugins/libSciberQuestToolKit.dylib",
    remote=False,
    ns=globals(),
)
inFile = os.path.abspath(sys.argv[1])
outImage = os.path.abspath(sys.argv[2])
myScript = "import os,sys\naPath='/Users/maderk/Dropbox/TIPL/src/Python/'\nif aPath not in sys.path: sys.path.append(aPath)\nimport pvtools as pvtools\n"
myScript += "filtList=[]\n"
myScript += "#filtList+=[('COUNT',lambda x: x>10)]\n"
myScript += "synList=[]\n"
myScript += "#synList+=[('Alignment',lambda x: 180/numpy.pi*pvtools.calcAlignment(x,'DIR_X','DIR_Y','DIR_Z'))]\n"
myScript += "aFile='" + inFile + "'\n"
myScript += "pvtools.ImportCSV(self,aFile,filtList,synList)\n"

RenderView2 = CreateRenderView()
RenderView2.LightSpecularColor = [1.0, 1.0, 1.0]
RenderView2.UseOutlineForLODRendering = 0
RenderView2.KeyLightAzimuth = 10.0
RenderView2.UseTexturedBackground = 0
RenderView2.UseLight = 1
RenderView2.CameraPosition = [
    1915.4881554020183,
    -1111.2928260752212,
    1307.4696104783361,
]
RenderView2.FillLightKFRatio = 3.0
RenderView2.Background2 = [0.0, 0.0, 0.16500000000000001]
RenderView2.FillLightAzimuth = -10.0
RenderView2.LODResolution = 0.5
RenderView2.BackgroundTexture = []
RenderView2.InteractionMode = "3D"
RenderView2.StencilCapable = 1
RenderView2.LightIntensity = 1.0
RenderView2.CameraFocalPoint = [
    217.61799573898313,
    210.34154129028323,
    261.12156295776367,
]
RenderView2.ImageReductionFactor = 2
RenderView2.CameraViewAngle = 30.0
RenderView2.CameraParallelScale = 422.94862817243114
RenderView2.EyeAngle = 2.0
RenderView2.HeadLightKHRatio = 3.0
RenderView2.StereoRender = 0
RenderView2.KeyLightIntensity = 0.75
RenderView2.BackLightAzimuth = 110.0
RenderView2.OrientationAxesInteractivity = 0
RenderView2.UseInteractiveRenderingForSceenshots = 0
RenderView2.UseOffscreenRendering = 0
RenderView2.Background = [0.31999694819562063, 0.34000152590218968, 0.42999923704890519]
RenderView2.UseOffscreenRenderingForScreenshots = 0
RenderView2.NonInteractiveRenderDelay = 0.0
RenderView2.CenterOfRotation = [
    217.61799573898315,
    210.3415412902832,
    261.12156295776367,
]
RenderView2.CameraParallelProjection = 0
RenderView2.CompressorConfig = "vtkSquirtCompressor 0 3"
RenderView2.HeadLightWarmth = 0.5
RenderView2.MaximumNumberOfPeels = 4
RenderView2.LightDiffuseColor = [1.0, 1.0, 1.0]
RenderView2.StereoType = "Red-Blue"
RenderView2.DepthPeeling = 1
RenderView2.BackLightKBRatio = 3.5
RenderView2.StereoCapableWindow = 1
RenderView2.CameraViewUp = [
    -0.39070747035828279,
    0.20799824119446281,
    0.8967075355243993,
]
RenderView2.LightType = "HeadLight"
RenderView2.LightAmbientColor = [1.0, 1.0, 1.0]
RenderView2.RemoteRenderThreshold = 20.0
RenderView2.CacheKey = 0.0
RenderView2.UseCache = 0
RenderView2.KeyLightElevation = 50.0
RenderView2.CenterAxesVisibility = 1
RenderView2.MaintainLuminance = 0
RenderView2.StillRenderImageReductionFactor = 1
RenderView2.BackLightWarmth = 0.5
RenderView2.FillLightElevation = -75.0
RenderView2.MultiSamples = 0
RenderView2.FillLightWarmth = 0.40000000000000002
RenderView2.AlphaBitPlanes = 1
RenderView2.LightSwitch = 0
RenderView2.OrientationAxesVisibility = 1
RenderView2.CameraClippingRange = [1554.7565699382133, 3452.9440977759409]
RenderView2.BackLightElevation = 0.0
RenderView2.ViewTime = 0.0
RenderView2.OrientationAxesOutlineColor = [1.0, 1.0, 1.0]
RenderView2.LODThreshold = 5.0
RenderView2.CollectGeometryThreshold = 100.0
RenderView2.UseGradientBackground = 0
RenderView2.KeyLightWarmth = 0.59999999999999998
RenderView2.OrientationAxesLabelColor = [1.0, 1.0, 1.0]

ProgrammableSource1 = ProgrammableSource(
    guiName="ProgrammableSource1",
    OutputDataSetType="vtkTable",
    PythonPath="",
    ScriptRequestInformation="",
    Script=myScript,
)

TableToPoints1 = TableToPoints(
    guiName="TableToPoints1",
    XColumn="POS_X",
    a2DPoints=0,
    ZColumn="POS_Z",
    YColumn="POS_Y",
    KeepAllDataArrays=0,
)

Glyph1 = Glyph(
    guiName="Glyph1",
    KeepRandomPoints=0,
    RandomMode=1,
    GlyphTransform="Transform2",
    GlyphType="Sphere",
    MaximumNumberofPoints=5000,
    ScaleMode="vector",
    MaskPoints=1,
    Vectors=["POINTS", "PCA1"],
    SetScaleFactor=44.742942444717798,
    Scalars=["POINTS", "AISO"],
    Orient=1,
)
Glyph1.GlyphType.StartTheta = 0.0
Glyph1.GlyphType.ThetaResolution = 8
Glyph1.GlyphTransform.Rotate = [0.0, 0.0, 0.0]
Glyph1.GlyphTransform.Translate = [0.0, 0.0, 0.0]
Glyph1.GlyphType.PhiResolution = 8
Glyph1.GlyphType.EndTheta = 360.0
Glyph1.GlyphType.EndPhi = 180.0
Glyph1.GlyphType.Center = [0.0, 0.0, 0.0]
Glyph1.GlyphType.Radius = 0.5
Glyph1.GlyphTransform.Scale = [1.0, 1.0, 1.0]
Glyph1.GlyphType.StartPhi = 0.0

SetActiveSource(TableToPoints1)
SQTensorGlyphs1 = SQTensorGlyphs(
    guiName="SQTensorGlyphs1",
    Symmetric=0,
    GlyphType="Superquadric",
    Scalars=["POINTS", "NEIGHBORS"],
    LimitScalingByEigenvalues=1,
    ColorGlyphs=1,
    Colorby="input scalars",
    MaxScaleFactor=100.0,
    ExtractEigenvalues=1,
    ScaleFactor=1.0,
    Tensors=["POINTS", "SHAPET_T"],
    ThreeGlyphs=0,
)
SQTensorGlyphs1.GlyphType.Toroidal = 0
SQTensorGlyphs1.GlyphType.Center = [0.0, 0.0, 0.0]
SQTensorGlyphs1.GlyphType.ThetaResolution = 16
SQTensorGlyphs1.GlyphType.Scale = [1.0, 1.0, 1.0]
SQTensorGlyphs1.GlyphType.PhiRoundness = 1.0
SQTensorGlyphs1.GlyphType.ThetaRoundness = 1.0
SQTensorGlyphs1.GlyphType.PhiResolution = 16
SQTensorGlyphs1.GlyphType.Size = 0.5
SQTensorGlyphs1.GlyphType.Thickness = 0.33329999999999999

a1_AISO_PiecewiseFunction = CreatePiecewiseFunction(
    Points=[0.0, 0.0, 0.5, 0.0, 1.0, 1.0, 0.5, 0.0]
)

a1_AMP_DSTRAIN_PiecewiseFunction = CreatePiecewiseFunction(
    Points=[0.0, 0.0, 0.5, 0.0, 1.0, 1.0, 0.5, 0.0]
)

a1_TR_STRAIN_PiecewiseFunction = CreatePiecewiseFunction(
    Points=[-1.0, 0.0, 0.5, 0.0, 1.0, 1.0, 0.5, 0.0]
)

a1_NEIGHBORS_PiecewiseFunction = CreatePiecewiseFunction(
    Points=[0.0, 0.0, 0.5, 0.0, 1.0, 1.0, 0.5, 0.0]
)

a1_AISO_PVLookupTable = GetLookupTableForArray(
    "AISO",
    1,
    Discretize=1,
    RGBPoints=[
        0.14027699828147888,
        0.23000000000000001,
        0.29899999999999999,
        0.754,
        1.0,
        0.70599999999999996,
        0.016,
        0.14999999999999999,
    ],
    UseLogScale=0,
    VectorComponent=0,
    NanColor=[0.25, 0.0, 0.0],
    NumberOfTableValues=256,
    EnableOpacityMapping=0,
    ColorSpace="Diverging",
    IndexedLookup=0,
    VectorMode="Magnitude",
    ScalarOpacityFunction=a1_AISO_PiecewiseFunction,
    HSVWrap=0,
    ScalarRangeInitialized=1.0,
    AllowDuplicateScalars=1,
    Annotations=[],
    LockScalarRange=0,
)

a1_AMP_DSTRAIN_PVLookupTable = GetLookupTableForArray(
    "AMP_DSTRAIN",
    1,
    Discretize=1,
    RGBPoints=[
        0.0,
        0.23000000000000001,
        0.29899999999999999,
        0.754,
        33.242908477783203,
        0.70599999999999996,
        0.016,
        0.14999999999999999,
    ],
    UseLogScale=0,
    VectorComponent=0,
    NanColor=[0.25, 0.0, 0.0],
    NumberOfTableValues=256,
    EnableOpacityMapping=0,
    ColorSpace="Diverging",
    IndexedLookup=0,
    VectorMode="Magnitude",
    ScalarOpacityFunction=a1_AMP_DSTRAIN_PiecewiseFunction,
    HSVWrap=0,
    ScalarRangeInitialized=1.0,
    AllowDuplicateScalars=1,
    Annotations=[],
    LockScalarRange=0,
)

a1_TR_STRAIN_PVLookupTable = GetLookupTableForArray(
    "TR_STRAIN",
    1,
    Discretize=1,
    RGBPoints=[
        -1.0,
        0.23000000000000001,
        0.29899999999999999,
        0.754,
        1.0,
        0.70599999999999996,
        0.016,
        0.14999999999999999,
    ],
    UseLogScale=0,
    VectorComponent=0,
    NanColor=[0.25, 0.0, 0.0],
    NumberOfTableValues=256,
    EnableOpacityMapping=0,
    ColorSpace="Diverging",
    IndexedLookup=0,
    VectorMode="Magnitude",
    ScalarOpacityFunction=a1_TR_STRAIN_PiecewiseFunction,
    HSVWrap=0,
    ScalarRangeInitialized=1.0,
    AllowDuplicateScalars=1,
    Annotations=[],
    LockScalarRange=1,
)

a1_NEIGHBORS_PVLookupTable = GetLookupTableForArray(
    "NEIGHBORS",
    1,
    Discretize=1,
    RGBPoints=[
        0.0,
        0.23000000000000001,
        0.29899999999999999,
        0.754,
        19.0,
        0.70599999999999996,
        0.016,
        0.14999999999999999,
    ],
    UseLogScale=0,
    VectorComponent=0,
    NanColor=[0.25, 0.0, 0.0],
    NumberOfTableValues=256,
    EnableOpacityMapping=0,
    ColorSpace="Diverging",
    IndexedLookup=0,
    VectorMode="Magnitude",
    ScalarOpacityFunction=a1_NEIGHBORS_PiecewiseFunction,
    HSVWrap=0,
    ScalarRangeInitialized=1.0,
    AllowDuplicateScalars=1,
    Annotations=[],
    LockScalarRange=0,
)

ScalarBarWidgetRepresentation1 = CreateScalarBar(
    Title="TR_STRAIN",
    Position2=[0.13, 0.5],
    TitleOpacity=1.0,
    TitleShadow=0,
    AutomaticLabelFormat=1,
    TitleFontSize=12,
    TitleColor=[1.0, 1.0, 1.0],
    AspectRatio=20.0,
    NumberOfLabels=5,
    ComponentTitle="",
    Resizable=1,
    TitleFontFamily="Arial",
    Visibility=0,
    LabelFontSize=12,
    LabelFontFamily="Arial",
    TitleItalic=0,
    Selectable=0,
    LabelItalic=0,
    Enabled=0,
    LabelColor=[1.0, 1.0, 1.0],
    Position=[0.87, 0.25],
    LabelBold=0,
    UseNonCompositedRenderer=1,
    LabelOpacity=1.0,
    TitleBold=0,
    LabelFormat="%-#6.3g",
    Orientation="Vertical",
    LabelShadow=0,
    LookupTable=a1_TR_STRAIN_PVLookupTable,
    Repositionable=1,
)
GetRenderView().Representations.append(ScalarBarWidgetRepresentation1)

ScalarBarWidgetRepresentation2 = CreateScalarBar(
    Title="NEIGHBORS",
    Position2=[0.13, 0.5],
    TitleOpacity=1.0,
    TitleShadow=0,
    AutomaticLabelFormat=1,
    TitleFontSize=12,
    TitleColor=[1.0, 1.0, 1.0],
    AspectRatio=20.0,
    NumberOfLabels=5,
    ComponentTitle="",
    Resizable=1,
    TitleFontFamily="Arial",
    Visibility=1,
    LabelFontSize=12,
    LabelFontFamily="Arial",
    TitleItalic=0,
    Selectable=0,
    LabelItalic=0,
    Enabled=1,
    LabelColor=[1.0, 1.0, 1.0],
    Position=[0.87, 0.25],
    LabelBold=0,
    UseNonCompositedRenderer=1,
    LabelOpacity=1.0,
    TitleBold=0,
    LabelFormat="%-#6.3g",
    Orientation="Vertical",
    LabelShadow=0,
    LookupTable=a1_NEIGHBORS_PVLookupTable,
    Repositionable=1,
)
GetRenderView().Representations.append(ScalarBarWidgetRepresentation2)


SetActiveSource(TableToPoints1)
SetActiveView(RenderView2)
DataRepresentation2 = Show()
DataRepresentation2.CubeAxesZAxisVisibility = 1
DataRepresentation2.SelectionPointLabelColor = [0.5, 0.5, 0.5]
DataRepresentation2.SelectionPointFieldDataArrayName = "AISO"
DataRepresentation2.SuppressLOD = 0
DataRepresentation2.CubeAxesXGridLines = 0
DataRepresentation2.CubeAxesYAxisTickVisibility = 1
DataRepresentation2.Position = [0.0, 0.0, 0.0]
DataRepresentation2.BackfaceRepresentation = "Follow Frontface"
DataRepresentation2.SelectionOpacity = 1.0
DataRepresentation2.SelectionPointLabelShadow = 0
DataRepresentation2.CubeAxesYGridLines = 0
DataRepresentation2.CubeAxesZAxisTickVisibility = 1
DataRepresentation2.OrientationMode = "Direction"
DataRepresentation2.Source.TipResolution = 6
DataRepresentation2.ScaleMode = "No Data Scaling Off"
DataRepresentation2.Diffuse = 1.0
DataRepresentation2.SelectionUseOutline = 0
DataRepresentation2.CubeAxesZTitle = "Z-Axis"
DataRepresentation2.Specular = 0.10000000000000001
DataRepresentation2.SelectionVisibility = 1
DataRepresentation2.InterpolateScalarsBeforeMapping = 1
DataRepresentation2.CustomRangeActive = [0, 0, 0]
DataRepresentation2.Origin = [0.0, 0.0, 0.0]
DataRepresentation2.Source.TipLength = 0.34999999999999998
DataRepresentation2.CubeAxesVisibility = 0
DataRepresentation2.Scale = [1.0, 1.0, 1.0]
DataRepresentation2.SelectionCellLabelJustification = "Left"
DataRepresentation2.DiffuseColor = [1.0, 1.0, 1.0]
DataRepresentation2.SelectionCellLabelOpacity = 1.0
DataRepresentation2.Source = "Arrow"
DataRepresentation2.Source.Invert = 0
DataRepresentation2.Masking = 0
DataRepresentation2.Opacity = 1.0
DataRepresentation2.LineWidth = 1.0
DataRepresentation2.MeshVisibility = 0
DataRepresentation2.Visibility = 0
DataRepresentation2.SelectionCellLabelFontSize = 18
DataRepresentation2.CubeAxesCornerOffset = 0.0
DataRepresentation2.SelectionPointLabelJustification = "Left"
DataRepresentation2.OriginalBoundsRangeActive = [0, 0, 0]
DataRepresentation2.SelectionPointLabelVisibility = 0
DataRepresentation2.SelectOrientationVectors = ""
DataRepresentation2.CubeAxesTickLocation = "Inside"
DataRepresentation2.BackfaceDiffuseColor = [1.0, 1.0, 1.0]
DataRepresentation2.CubeAxesYAxisVisibility = 1
DataRepresentation2.SelectionPointLabelFontFamily = "Arial"
DataRepresentation2.Source.ShaftResolution = 6
DataRepresentation2.CubeAxesUseDefaultYTitle = 1
DataRepresentation2.SelectScaleArray = ""
DataRepresentation2.CubeAxesYTitle = "Y-Axis"
DataRepresentation2.ColorAttributeType = "POINT_DATA"
DataRepresentation2.AxesOrigin = [0.0, 0.0, 0.0]
DataRepresentation2.UserTransform = [
    1.0,
    0.0,
    0.0,
    0.0,
    0.0,
    1.0,
    0.0,
    0.0,
    0.0,
    0.0,
    1.0,
    0.0,
    0.0,
    0.0,
    0.0,
    1.0,
]
DataRepresentation2.SpecularPower = 100.0
DataRepresentation2.Texture = []
DataRepresentation2.SelectionCellLabelShadow = 0
DataRepresentation2.AmbientColor = [1.0, 1.0, 1.0]
DataRepresentation2.MapScalars = 1
DataRepresentation2.PointSize = 2.0
DataRepresentation2.CubeAxesUseDefaultXTitle = 1
DataRepresentation2.SelectionCellLabelFormat = ""
DataRepresentation2.Scaling = 0
DataRepresentation2.StaticMode = 0
DataRepresentation2.SelectionCellLabelColor = [0.0, 1.0, 0.0]
DataRepresentation2.Source.TipRadius = 0.10000000000000001
DataRepresentation2.EdgeColor = [0.0, 0.0, 0.50000762951094835]
DataRepresentation2.CubeAxesXAxisTickVisibility = 1
DataRepresentation2.SelectionCellLabelVisibility = 0
DataRepresentation2.NonlinearSubdivisionLevel = 1
DataRepresentation2.CubeAxesColor = [1.0, 1.0, 1.0]
DataRepresentation2.Representation = "Points"
DataRepresentation2.CustomRange = [0.0, 1.0, 0.0, 1.0, 0.0, 1.0]
DataRepresentation2.CustomBounds = [0.0, 1.0, 0.0, 1.0, 0.0, 1.0]
DataRepresentation2.Orientation = [0.0, 0.0, 0.0]
DataRepresentation2.CubeAxesXTitle = "X-Axis"
DataRepresentation2.CubeAxesInertia = 1
DataRepresentation2.BackfaceOpacity = 1.0
DataRepresentation2.SelectionPointLabelFontSize = 18
DataRepresentation2.SelectionCellFieldDataArrayName = "vtkOriginalCellIds"
DataRepresentation2.SelectionColor = [1.0, 0.0, 1.0]
DataRepresentation2.Ambient = 0.0
DataRepresentation2.CubeAxesXAxisMinorTickVisibility = 1
DataRepresentation2.ScaleFactor = 44.742973518371585
DataRepresentation2.BackfaceAmbientColor = [1.0, 1.0, 1.0]
DataRepresentation2.Source.ShaftRadius = 0.029999999999999999
DataRepresentation2.SelectMaskArray = ""
DataRepresentation2.SelectionLineWidth = 2.0
DataRepresentation2.CubeAxesZAxisMinorTickVisibility = 1
DataRepresentation2.CubeAxesXAxisVisibility = 1
DataRepresentation2.Interpolation = "Gouraud"
DataRepresentation2.SelectionCellLabelFontFamily = "Arial"
DataRepresentation2.SelectionCellLabelItalic = 0
DataRepresentation2.CubeAxesYAxisMinorTickVisibility = 1
DataRepresentation2.CubeAxesZGridLines = 0
DataRepresentation2.SelectionPointLabelFormat = ""
DataRepresentation2.SelectionPointLabelOpacity = 1.0
DataRepresentation2.UseAxesOrigin = 0
DataRepresentation2.CubeAxesFlyMode = "Closest Triad"
DataRepresentation2.Pickable = 1
DataRepresentation2.CustomBoundsActive = [0, 0, 0]
DataRepresentation2.CubeAxesGridLineLocation = "All Faces"
DataRepresentation2.SelectionRepresentation = "Wireframe"
DataRepresentation2.SelectionPointLabelBold = 0
DataRepresentation2.ColorArrayName = ""
DataRepresentation2.SelectionPointLabelItalic = 0
DataRepresentation2.AllowSpecularHighlightingWithScalarColoring = 0
DataRepresentation2.SpecularColor = [1.0, 1.0, 1.0]
DataRepresentation2.CubeAxesUseDefaultZTitle = 1
DataRepresentation2.LookupTable = []
DataRepresentation2.SelectionPointSize = 5.0
DataRepresentation2.SelectionCellLabelBold = 0
DataRepresentation2.Orient = 0

SetActiveSource(Glyph1)
DataRepresentation3 = Show()
DataRepresentation3.CubeAxesZAxisVisibility = 1
DataRepresentation3.SelectionPointLabelColor = [0.5, 0.5, 0.5]
DataRepresentation3.SelectionPointFieldDataArrayName = "AISO"
DataRepresentation3.SuppressLOD = 0
DataRepresentation3.CubeAxesXGridLines = 0
DataRepresentation3.CubeAxesYAxisTickVisibility = 1
DataRepresentation3.Position = [0.0, 0.0, 0.0]
DataRepresentation3.BackfaceRepresentation = "Follow Frontface"
DataRepresentation3.SelectionOpacity = 1.0
DataRepresentation3.SelectionPointLabelShadow = 0
DataRepresentation3.CubeAxesYGridLines = 0
DataRepresentation3.CubeAxesZAxisTickVisibility = 1
DataRepresentation3.OrientationMode = "Direction"
DataRepresentation3.Source.TipResolution = 6
DataRepresentation3.ScaleMode = "No Data Scaling Off"
DataRepresentation3.Diffuse = 1.0
DataRepresentation3.SelectionUseOutline = 0
DataRepresentation3.CubeAxesZTitle = "Z-Axis"
DataRepresentation3.Specular = 0.10000000000000001
DataRepresentation3.SelectionVisibility = 1
DataRepresentation3.InterpolateScalarsBeforeMapping = 1
DataRepresentation3.CustomRangeActive = [0, 0, 0]
DataRepresentation3.Origin = [0.0, 0.0, 0.0]
DataRepresentation3.Source.TipLength = 0.34999999999999998
DataRepresentation3.CubeAxesVisibility = 0
DataRepresentation3.Scale = [1.0, 1.0, 1.0]
DataRepresentation3.SelectionCellLabelJustification = "Left"
DataRepresentation3.DiffuseColor = [1.0, 1.0, 1.0]
DataRepresentation3.SelectionCellLabelOpacity = 1.0
DataRepresentation3.Source = "Arrow"
DataRepresentation3.Source.Invert = 0
DataRepresentation3.Masking = 0
DataRepresentation3.Opacity = 1.0
DataRepresentation3.LineWidth = 1.0
DataRepresentation3.MeshVisibility = 0
DataRepresentation3.Visibility = 0
DataRepresentation3.SelectionCellLabelFontSize = 18
DataRepresentation3.CubeAxesCornerOffset = 0.0
DataRepresentation3.SelectionPointLabelJustification = "Left"
DataRepresentation3.OriginalBoundsRangeActive = [0, 0, 0]
DataRepresentation3.SelectionPointLabelVisibility = 0
DataRepresentation3.SelectOrientationVectors = ""
DataRepresentation3.CubeAxesTickLocation = "Inside"
DataRepresentation3.BackfaceDiffuseColor = [1.0, 1.0, 1.0]
DataRepresentation3.CubeAxesYAxisVisibility = 1
DataRepresentation3.SelectionPointLabelFontFamily = "Arial"
DataRepresentation3.Source.ShaftResolution = 6
DataRepresentation3.CubeAxesUseDefaultYTitle = 1
DataRepresentation3.SelectScaleArray = ""
DataRepresentation3.CubeAxesYTitle = "Y-Axis"
DataRepresentation3.ColorAttributeType = "POINT_DATA"
DataRepresentation3.AxesOrigin = [0.0, 0.0, 0.0]
DataRepresentation3.UserTransform = [
    1.0,
    0.0,
    0.0,
    0.0,
    0.0,
    1.0,
    0.0,
    0.0,
    0.0,
    0.0,
    1.0,
    0.0,
    0.0,
    0.0,
    0.0,
    1.0,
]
DataRepresentation3.SpecularPower = 100.0
DataRepresentation3.Texture = []
DataRepresentation3.SelectionCellLabelShadow = 0
DataRepresentation3.AmbientColor = [1.0, 1.0, 1.0]
DataRepresentation3.MapScalars = 1
DataRepresentation3.PointSize = 2.0
DataRepresentation3.CubeAxesUseDefaultXTitle = 1
DataRepresentation3.SelectionCellLabelFormat = ""
DataRepresentation3.Scaling = 0
DataRepresentation3.StaticMode = 0
DataRepresentation3.SelectionCellLabelColor = [0.0, 1.0, 0.0]
DataRepresentation3.Source.TipRadius = 0.10000000000000001
DataRepresentation3.EdgeColor = [0.0, 0.0, 0.50000762951094835]
DataRepresentation3.CubeAxesXAxisTickVisibility = 1
DataRepresentation3.SelectionCellLabelVisibility = 0
DataRepresentation3.NonlinearSubdivisionLevel = 1
DataRepresentation3.CubeAxesColor = [1.0, 1.0, 1.0]
DataRepresentation3.Representation = "Surface"
DataRepresentation3.CustomRange = [0.0, 1.0, 0.0, 1.0, 0.0, 1.0]
DataRepresentation3.CustomBounds = [0.0, 1.0, 0.0, 1.0, 0.0, 1.0]
DataRepresentation3.Orientation = [0.0, 0.0, 0.0]
DataRepresentation3.CubeAxesXTitle = "X-Axis"
DataRepresentation3.CubeAxesInertia = 1
DataRepresentation3.BackfaceOpacity = 1.0
DataRepresentation3.SelectionPointLabelFontSize = 18
DataRepresentation3.SelectionCellFieldDataArrayName = "vtkOriginalCellIds"
DataRepresentation3.SelectionColor = [1.0, 0.0, 1.0]
DataRepresentation3.Ambient = 0.0
DataRepresentation3.CubeAxesXAxisMinorTickVisibility = 1
DataRepresentation3.ScaleFactor = 49.217264223098759
DataRepresentation3.BackfaceAmbientColor = [1.0, 1.0, 1.0]
DataRepresentation3.Source.ShaftRadius = 0.029999999999999999
DataRepresentation3.SelectMaskArray = ""
DataRepresentation3.SelectionLineWidth = 2.0
DataRepresentation3.CubeAxesZAxisMinorTickVisibility = 1
DataRepresentation3.CubeAxesXAxisVisibility = 1
DataRepresentation3.Interpolation = "Gouraud"
DataRepresentation3.SelectionCellLabelFontFamily = "Arial"
DataRepresentation3.SelectionCellLabelItalic = 0
DataRepresentation3.CubeAxesYAxisMinorTickVisibility = 1
DataRepresentation3.CubeAxesZGridLines = 0
DataRepresentation3.SelectionPointLabelFormat = ""
DataRepresentation3.SelectionPointLabelOpacity = 1.0
DataRepresentation3.UseAxesOrigin = 0
DataRepresentation3.CubeAxesFlyMode = "Closest Triad"
DataRepresentation3.Pickable = 1
DataRepresentation3.CustomBoundsActive = [0, 0, 0]
DataRepresentation3.CubeAxesGridLineLocation = "All Faces"
DataRepresentation3.SelectionRepresentation = "Wireframe"
DataRepresentation3.SelectionPointLabelBold = 0
DataRepresentation3.ColorArrayName = "NEIGHBORS"
DataRepresentation3.SelectionPointLabelItalic = 0
DataRepresentation3.AllowSpecularHighlightingWithScalarColoring = 0
DataRepresentation3.SpecularColor = [1.0, 1.0, 1.0]
DataRepresentation3.CubeAxesUseDefaultZTitle = 1
DataRepresentation3.LookupTable = a1_AMP_DSTRAIN_PVLookupTable
DataRepresentation3.SelectionPointSize = 5.0
DataRepresentation3.SelectionCellLabelBold = 0
DataRepresentation3.Orient = 0

SetActiveSource(SQTensorGlyphs1)
DataRepresentation4 = Show()
DataRepresentation4.CubeAxesZAxisVisibility = 1
DataRepresentation4.SelectionPointLabelColor = [0.5, 0.5, 0.5]
DataRepresentation4.SelectionPointFieldDataArrayName = "AISO"
DataRepresentation4.SuppressLOD = 0
DataRepresentation4.CubeAxesXGridLines = 0
DataRepresentation4.CubeAxesYAxisTickVisibility = 1
DataRepresentation4.Position = [0.0, 0.0, 0.0]
DataRepresentation4.BackfaceRepresentation = "Follow Frontface"
DataRepresentation4.SelectionOpacity = 1.0
DataRepresentation4.SelectionPointLabelShadow = 0
DataRepresentation4.CubeAxesYGridLines = 0
DataRepresentation4.CubeAxesZAxisTickVisibility = 1
DataRepresentation4.OrientationMode = "Direction"
DataRepresentation4.Source.TipResolution = 6
DataRepresentation4.ScaleMode = "No Data Scaling Off"
DataRepresentation4.Diffuse = 1.0
DataRepresentation4.SelectionUseOutline = 0
DataRepresentation4.CubeAxesZTitle = "Z-Axis"
DataRepresentation4.Specular = 0.10000000000000001
DataRepresentation4.SelectionVisibility = 1
DataRepresentation4.InterpolateScalarsBeforeMapping = 1
DataRepresentation4.CustomRangeActive = [0, 0, 0]
DataRepresentation4.Origin = [0.0, 0.0, 0.0]
DataRepresentation4.Source.TipLength = 0.34999999999999998

DataRepresentation4.CubeAxesVisibility = 0
DataRepresentation4.Scale = [1.0, 1.0, 1.0]
DataRepresentation4.SelectionCellLabelJustification = "Left"
DataRepresentation4.DiffuseColor = [1.0, 1.0, 1.0]
DataRepresentation4.SelectionCellLabelOpacity = 1.0
DataRepresentation4.Source = "Arrow"
DataRepresentation4.Source.Invert = 0
DataRepresentation4.Masking = 0
DataRepresentation4.Opacity = 1.0
DataRepresentation4.LineWidth = 1.0
DataRepresentation4.MeshVisibility = 0
DataRepresentation4.Visibility = 1
DataRepresentation4.SelectionCellLabelFontSize = 18
DataRepresentation4.CubeAxesCornerOffset = 0.0
DataRepresentation4.SelectionPointLabelJustification = "Left"

DataRepresentation4.OriginalBoundsRangeActive = [0, 0, 0]
DataRepresentation4.SelectionPointLabelVisibility = 0
DataRepresentation4.SelectOrientationVectors = ""
DataRepresentation4.CubeAxesTickLocation = "Inside"
DataRepresentation4.BackfaceDiffuseColor = [1.0, 1.0, 1.0]
DataRepresentation4.CubeAxesYAxisVisibility = 1
DataRepresentation4.SelectionPointLabelFontFamily = "Arial"
DataRepresentation4.Source.ShaftResolution = 6
DataRepresentation4.CubeAxesUseDefaultYTitle = 1
DataRepresentation4.SelectScaleArray = ""
DataRepresentation4.CubeAxesYTitle = "Y-Axis"
DataRepresentation4.ColorAttributeType = "POINT_DATA"
DataRepresentation4.AxesOrigin = [0.0, 0.0, 0.0]

DataRepresentation4.SpecularPower = 100.0
DataRepresentation4.Texture = []
DataRepresentation4.SelectionCellLabelShadow = 0
DataRepresentation4.AmbientColor = [1.0, 1.0, 1.0]
DataRepresentation4.MapScalars = 1
DataRepresentation4.PointSize = 2.0
DataRepresentation4.CubeAxesUseDefaultXTitle = 1
DataRepresentation4.SelectionCellLabelFormat = ""
DataRepresentation4.Scaling = 0
DataRepresentation4.StaticMode = 0
DataRepresentation4.SelectionCellLabelColor = [0.0, 1.0, 0.0]
DataRepresentation4.Source.TipRadius = 0.10000000000000001
DataRepresentation4.EdgeColor = [0.0, 0.0, 0.50000762951094835]
DataRepresentation4.CubeAxesXAxisTickVisibility = 1
DataRepresentation4.SelectionCellLabelVisibility = 0
DataRepresentation4.NonlinearSubdivisionLevel = 1
DataRepresentation4.CubeAxesColor = [1.0, 1.0, 1.0]
DataRepresentation4.Representation = "Surface"
DataRepresentation4.CustomRange = [0.0, 1.0, 0.0, 1.0, 0.0, 1.0]
DataRepresentation4.CustomBounds = [0.0, 1.0, 0.0, 1.0, 0.0, 1.0]
DataRepresentation4.Orientation = [0.0, 0.0, 0.0]
DataRepresentation4.CubeAxesXTitle = "X-Axis"
DataRepresentation4.CubeAxesInertia = 1
DataRepresentation4.BackfaceOpacity = 1.0
DataRepresentation4.SelectionPointLabelFontSize = 18
DataRepresentation4.SelectionCellFieldDataArrayName = "vtkOriginalCellIds"
DataRepresentation4.SelectionColor = [1.0, 0.0, 1.0]
DataRepresentation4.Ambient = 0.0
DataRepresentation4.CubeAxesXAxisMinorTickVisibility = 1
DataRepresentation4.ScaleFactor = 319.3362915039063
DataRepresentation4.BackfaceAmbientColor = [1.0, 1.0, 1.0]
DataRepresentation4.Source.ShaftRadius = 0.029999999999999999
DataRepresentation4.SelectMaskArray = ""
DataRepresentation4.SelectionLineWidth = 2.0
DataRepresentation4.CubeAxesZAxisMinorTickVisibility = 1
DataRepresentation4.CubeAxesXAxisVisibility = 1
DataRepresentation4.Interpolation = "Gouraud"
DataRepresentation4.SelectionCellLabelFontFamily = "Arial"
DataRepresentation4.SelectionCellLabelItalic = 0
DataRepresentation4.CubeAxesYAxisMinorTickVisibility = 1
DataRepresentation4.CubeAxesZGridLines = 0
DataRepresentation4.SelectionPointLabelFormat = ""
DataRepresentation4.SelectionPointLabelOpacity = 1.0
DataRepresentation4.UseAxesOrigin = 0
DataRepresentation4.CubeAxesFlyMode = "Closest Triad"
DataRepresentation4.Pickable = 1
DataRepresentation4.CustomBoundsActive = [0, 0, 0]
DataRepresentation4.CubeAxesGridLineLocation = "All Faces"
DataRepresentation4.SelectionRepresentation = "Wireframe"
DataRepresentation4.SelectionPointLabelBold = 0
DataRepresentation4.UserTransform = [
    1.0,
    0.0,
    0.0,
    0.0,
    0.0,
    1.0,
    0.0,
    0.0,
    0.0,
    0.0,
    1.0,
    0.0,
    0.0,
    0.0,
    0.0,
    1.0,
]
DataRepresentation4.ColorArrayName = "NEIGHBORS"
DataRepresentation4.SelectionPointLabelItalic = 0
DataRepresentation4.AllowSpecularHighlightingWithScalarColoring = 0
DataRepresentation4.SpecularColor = [1.0, 1.0, 1.0]
DataRepresentation4.CubeAxesUseDefaultZTitle = 1
DataRepresentation4.LookupTable = a1_NEIGHBORS_PVLookupTable
DataRepresentation4.SelectionPointSize = 5.0
DataRepresentation4.SelectionCellLabelBold = 0
DataRepresentation4.Orient = 0

Render()
RenderView = GetRenderView()  # set camera properties for current view
RenderView.ViewSize = [1024, 1024]
RenderView.WriteImage(outImage, "vtkPNGWriter", 1)
