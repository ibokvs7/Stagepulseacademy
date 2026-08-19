package tr.stagepulse.tools;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.LightManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Scene;
import com.google.android.filament.SwapChain;
import com.google.android.filament.TransformManager;
import com.google.android.filament.View;
import com.google.android.filament.Viewport;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.UbershaderProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int CREATE_RIDER = 4101;
    private static final int CREATE_PROJECT = 4102;
    private static final int OPEN_PROJECT = 4103;

    private SceneRenderer scene;
    private TextView status;
    private EditText stageW, stageD, stageH, xField, yField, zField, rxField, ryField, rzField;
    private byte[] pendingDocument;

    private static final String[] LIBRARY = {
            "LINE ARRAY 4", "LINE ARRAY 8", "SUB DOUBLE 18", "SUB SINGLE 18", "MONITOR 12", "MONITOR 15",
            "DRUM KIT", "GUITAR", "BASS", "KEYS", "PIANO", "KANUN", "BAGLAMA", "UD", "KEMAN", "VIOLA", "CELLO",
            "DARBUKA", "NEY", "KLARNET", "SAKSAFON", "TROMPET", "TROMBON", "AKORDEON", "PERCUSSION", "VOCAL MIC",
            "IEM", "DI BOX", "TRUSS", "MOVING HEAD", "PAR", "LED BAR", "FOH CONSOLE", "MONITOR CONSOLE", "LAPTOP",
            "MIC STAND", "SPEAKER STAND", "LED WALL", "RISER", "BARRIER", "PC", "GENERATOR", "DELAY TOWER"
    };

    static { com.google.android.filament.Filament.init(); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8, 11, 16));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("STAGEPULSE • SYSTEM DESIGN", 18, Color.WHITE);
        header.addView(title, new LinearLayout.LayoutParams(0, 58, 1));
        Button rider = button("RIDER PDF"); rider.setOnClickListener(v -> createRiderPdf());
        Button save = button("SAVE"); save.setOnClickListener(v -> createProjectJson());
        Button open = button("OPEN"); open.setOnClickListener(v -> openProject());
        header.addView(rider, new LinearLayout.LayoutParams(110, 58));
        header.addView(save, new LinearLayout.LayoutParams(90, 58));
        header.addView(open, new LinearLayout.LayoutParams(90, 58));
        root.addView(header);

        HorizontalScrollView libraryScroll = new HorizontalScrollView(this);
        LinearLayout library = new LinearLayout(this);
        library.setOrientation(LinearLayout.HORIZONTAL);
        for (String item : LIBRARY) {
            Button b = button(item); b.setTextSize(8);
            b.setOnClickListener(v -> scene.addEquipment(item));
            library.addView(b, new LinearLayout.LayoutParams(112, 54));
        }
        libraryScroll.addView(library);
        root.addView(libraryScroll, new LinearLayout.LayoutParams(-1, 58));

        scene = new SceneRenderer(this);
        root.addView(scene, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout inspector = new LinearLayout(this);
        inspector.setOrientation(LinearLayout.VERTICAL);
        inspector.setPadding(6, 4, 6, 4);
        inspector.setBackgroundColor(Color.rgb(17, 22, 29));

        HorizontalScrollView stageScroll = new HorizontalScrollView(this);
        LinearLayout stageRow = new LinearLayout(this);
        stageW = field("W", "12"); stageD = field("D", "8"); stageH = field("H", "1");
        stageRow.addView(stageW, new LinearLayout.LayoutParams(90, 46));
        stageRow.addView(stageD, new LinearLayout.LayoutParams(90, 46));
        stageRow.addView(stageH, new LinearLayout.LayoutParams(90, 46));
        Button applyStage = button("APPLY STAGE"); applyStage.setOnClickListener(v -> applyStage());
        Button listener = button("LISTENER"); listener.setOnClickListener(v -> scene.addListener());
        Button heat = button("HEATMAP"); heat.setOnClickListener(v -> status.setText(scene.heatmapReport()));
        stageRow.addView(applyStage, new LinearLayout.LayoutParams(120, 46));
        stageRow.addView(listener, new LinearLayout.LayoutParams(95, 46));
        stageRow.addView(heat, new LinearLayout.LayoutParams(100, 46));
        stageScroll.addView(stageRow); inspector.addView(stageScroll, new LinearLayout.LayoutParams(-1, 48));

        HorizontalScrollView transformScroll = new HorizontalScrollView(this);
        LinearLayout transformRow = new LinearLayout(this);
        xField = field("X", "0"); yField = field("Y", "0"); zField = field("Z", "0");
        rxField = field("RX", "0"); ryField = field("RY", "0"); rzField = field("RZ", "0");
        transformRow.addView(xField, new LinearLayout.LayoutParams(78, 46));
        transformRow.addView(yField, new LinearLayout.LayoutParams(78, 46));
        transformRow.addView(zField, new LinearLayout.LayoutParams(78, 46));
        transformRow.addView(rxField, new LinearLayout.LayoutParams(78, 46));
        transformRow.addView(ryField, new LinearLayout.LayoutParams(78, 46));
        transformRow.addView(rzField, new LinearLayout.LayoutParams(78, 46));
        Button apply = button("APPLY"); apply.setOnClickListener(v -> applyObject());
        Button rotL = button("↺"); rotL.setOnClickListener(v -> scene.rotateSelected(-5));
        Button rotR = button("↻"); rotR.setOnClickListener(v -> scene.rotateSelected(5));
        Button next = button("NEXT"); next.setOnClickListener(v -> scene.selectNext());
        Button mode = button("MOVE"); mode.setOnClickListener(v -> { scene.moveMode = !scene.moveMode; mode.setText(scene.moveMode ? "MOVE" : "CAMERA"); });
        Button del = button("DELETE"); del.setOnClickListener(v -> scene.deleteSelected());
        Button undo = button("UNDO"); undo.setOnClickListener(v -> scene.undo());
        Button redo = button("REDO"); redo.setOnClickListener(v -> scene.redo());
        Button spl = button("SPL"); spl.setOnClickListener(v -> status.setText(scene.splReport()));
        transformRow.addView(apply, new LinearLayout.LayoutParams(90, 46));
        transformRow.addView(rotL, new LinearLayout.LayoutParams(60, 46));
        transformRow.addView(rotR, new LinearLayout.LayoutParams(60, 46));
        transformRow.addView(next, new LinearLayout.LayoutParams(75, 46));
        transformRow.addView(mode, new LinearLayout.LayoutParams(80, 46));
        transformRow.addView(del, new LinearLayout.LayoutParams(80, 46));
        transformRow.addView(undo, new LinearLayout.LayoutParams(75, 46));
        transformRow.addView(redo, new LinearLayout.LayoutParams(75, 46));
        transformRow.addView(spl, new LinearLayout.LayoutParams(70, 46));
        transformScroll.addView(transformRow); inspector.addView(transformScroll, new LinearLayout.LayoutParams(-1, 48));

        status = text("3D hazır • MOVE modunda objeye dokunup sürükleyin • iki parmak zoom • CAMERA modunda orbit", 12, Color.rgb(205, 214, 224));
        inspector.addView(status, new LinearLayout.LayoutParams(-1, 42));
        root.addView(inspector, new LinearLayout.LayoutParams(-1, 142));
        setContentView(root);
    }

    private void applyStage() {
        scene.stageW = Math.max(1, number(stageW, 12));
        scene.stageD = Math.max(1, number(stageD, 8));
        scene.stageH = Math.max(.1f, number(stageH, 1));
        scene.applyStageModel(); scene.saveUndo(); scene.invalidate();
    }

    private void applyObject() {
        Equipment e = scene.selected();
        if (e == null) { status.setText("Önce ekipman seçin."); return; }
        scene.saveUndo();
        e.x = number(xField, e.x); e.y = number(yField, e.y); e.z = number(zField, e.z);
        e.rotationX = number(rxField, e.rotationX); e.rotationY = number(ryField, e.rotationY); e.rotationZ = number(rzField, e.rotationZ);
        scene.applyTransform(e); scene.invalidate();
    }

    private void syncFields(Equipment e) {
        if (e == null) return;
        xField.setText(fmt(e.x)); yField.setText(fmt(e.y)); zField.setText(fmt(e.z));
        rxField.setText(fmt(e.rotationX)); ryField.setText(fmt(e.rotationY)); rzField.setText(fmt(e.rotationZ));
    }

    private void createRiderPdf() {
        try {
            pendingDocument = scene.buildRiderPdf();
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_TITLE, "StagePulse-Rider.pdf"); startActivityForResult(i, CREATE_RIDER);
        } catch (Exception e) { status.setText("Rider oluşturulamadı: " + e.getMessage()); }
    }

    private void createProjectJson() {
        pendingDocument = scene.exportProject().getBytes(StandardCharsets.UTF_8);
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "StagePulse-project.json"); startActivityForResult(i, CREATE_PROJECT);
    }

    private void openProject() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/json"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, OPEN_PROJECT);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            Uri uri = data.getData();
            if (requestCode == OPEN_PROJECT) {
                InputStream in = getContentResolver().openInputStream(uri); String json = new String(readAll(in), StandardCharsets.UTF_8); in.close();
                scene.importProject(json); status.setText("Proje açıldı."); return;
            }
            if (pendingDocument != null) {
                OutputStream out = getContentResolver().openOutputStream(uri); if (out != null) { out.write(pendingDocument); out.flush(); out.close(); }
                status.setText("Dosya kaydedildi.");
            }
        } catch (Exception e) { status.setText("Dosya işlemi başarısız: " + e.getMessage()); }
        pendingDocument = null;
    }

    static String fmt(float v) { return String.format(Locale.US, "%.2f", v); }
    static float number(EditText e, float fallback) { try { return Float.parseFloat(e.getText().toString().replace(',', '.')); } catch (Exception ex) { return fallback; } }
    private TextView text(String s, int size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); t.setPadding(8,0,8,0); return t; }
    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setAllCaps(false); return b; }
    private EditText field(String hint, String value) { EditText e = new EditText(this); e.setHint(hint); e.setText(value); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setTextSize(11); e.setSingleLine(true); e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); return e; }

    public static class Equipment {
        String type, modelFile, modelName, category;
        float x,y,z,rotationX,rotationY,rotationZ,width,height,depth,sensitivity,power,maxSPL,coverage;
        FilamentAsset asset;
        Equipment(String type) { this.type=type; this.modelFile=modelFor(type); configure(type); }
        private void configure(String t) {
            width=1;height=1;depth=1;sensitivity=96;power=500;maxSPL=125;coverage=90; category="EQUIPMENT"; modelName="StagePulse Reference";
            if(t.startsWith("LINE ARRAY")){width=1;height=.35f;depth=.65f;sensitivity=100;power=1600;maxSPL=142;coverage=90;category="PA";modelName=t; y=5;}
            else if(t.equals("SUB DOUBLE 18")){width=1.2f;height=1;depth=1;sensitivity=99;power=2400;maxSPL=139;coverage=120;category="PA";modelName="StagePulse SUB-D18 REF";}
            else if(t.equals("SUB SINGLE 18")){width=1;height=1;depth=.85f;sensitivity=98;power=1200;maxSPL=136;coverage=120;category="PA";modelName="StagePulse SUB-S18 REF";}
            else if(t.equals("MONITOR 12")){width=.75f;height=.35f;depth=.9f;sensitivity=96;power=700;maxSPL=130;coverage=80;category="MONITOR";modelName="StagePulse M12 REF";}
            else if(t.equals("MONITOR 15")){width=.9f;height=.42f;depth=1.05f;sensitivity=97;power=900;maxSPL=132;coverage=80;category="MONITOR";modelName="StagePulse M15 REF";}
            else if(t.equals("TRUSS")){width=4;height=.12f;depth=.12f;category="RIGGING";y=4;}
            else if(t.equals("MOVING HEAD")){width=.45f;height=.6f;depth=.35f;category="LIGHTING";y=4;}
            else if(t.equals("LED WALL")){width=5;height=3;depth=.2f;category="VIDEO";y=1.5f;}
            else if(t.equals("RISER")){width=3;height=.5f;depth=2;category="STAGE";}
            else if(t.equals("BARRIER")){width=2;height=1.1f;depth=.12f;category="SAFETY";}
            else if(t.equals("DRUM KIT")){width=2.1f;height=1.5f;depth=1.8f;category="INSTRUMENT";}
            else if(t.equals("GUITAR")){width=.45f;height=1.2f;depth=.35f;category="INSTRUMENT";}
            else if(t.equals("BASS")){width=.45f;height=1.3f;depth=.35f;category="INSTRUMENT";}
            else if(t.equals("KEYS")||t.equals("PIANO")){width=t.equals("PIANO")?1.5f:1.5f;height=.45f;depth=.5f;category="INSTRUMENT";}
            else if(t.equals("KANUN")){width=1.1f;height=.28f;depth=.55f;category="INSTRUMENT";}
            else if(t.equals("KEMAN")||t.equals("VIOLA")){width=.25f;height=.9f;depth=.2f;category="INSTRUMENT";}
            else if(t.equals("BAGLAMA")||t.equals("UD")){width=.45f;height=1.1f;depth=.32f;category="INSTRUMENT";}
            else if(t.equals("CELLO")){width=.5f;height=1.25f;depth=.35f;category="INSTRUMENT";}
            else if(t.equals("DARBUKA")){width=.6f;height=.7f;depth=.6f;category="INSTRUMENT";}
            else if(t.equals("NEY")||t.equals("KLARNET")){width=.15f;height=1.1f;depth=.15f;category="INSTRUMENT";}
            else if(t.equals("SAKSAFON")){width=.5f;height=1;depth=.35f;category="INSTRUMENT";}
            else if(t.equals("TROMPET")||t.equals("TROMBON")){width=.5f;height=1;depth=.3f;category="INSTRUMENT";}
            else if(t.equals("AKORDEON")){width=.6f;height=.8f;depth=.3f;category="INSTRUMENT";}
            else if(t.equals("PERCUSSION")){width=1;height=.8f;depth=.7f;category="INSTRUMENT";}
            else if(t.equals("IEM")){width=.3f;height=.12f;depth=.18f;category="MONITOR";}
            else if(t.equals("VOCAL MIC")){width=.1f;height=.25f;depth=.1f;category="MIC";}
            else if(t.equals("DI BOX")){width=.2f;height=.08f;depth=.12f;category="AUDIO";}
            else if(t.equals("MIC STAND")){width=.2f;height=1.7f;depth=.2f;category="MIC";}
            else if(t.equals("SPEAKER STAND")){width=.35f;height=2.3f;depth=.35f;category="RIGGING";y=2.3f;}
            else if(t.equals("FOH CONSOLE")||t.equals("MONITOR CONSOLE")){width=1.8f;height=.45f;depth=.85f;category="CONTROL";}
            else if(t.equals("LAPTOP")){width=.55f;height=.35f;depth=.4f;category="CONTROL";}
            else if(t.equals("PC")){width=.5f;height=.35f;depth=.3f;category="CONTROL";}
            else if(t.equals("GENERATOR")){width=1.5f;height=1.2f;depth=.8f;category="POWER";}
            else if(t.equals("DELAY TOWER")){width=1;height=3;depth=1;category="PA";y=3;}
        }
        static String modelFor(String t) {
            if(t.equals("LINE ARRAY 4"))return "models/linearray4.glb"; if(t.equals("LINE ARRAY 8"))return "models/linearray8.glb";
            if(t.equals("SUB DOUBLE 18"))return "models/sub_double18.glb"; if(t.equals("SUB SINGLE 18"))return "models/sub_single18.glb";
            if(t.equals("MONITOR 12"))return "models/monitor12.glb"; if(t.equals("MONITOR 15"))return "models/monitor15.glb";
            if(t.equals("DRUM KIT"))return "models/drumkit.glb"; if(t.equals("GUITAR"))return "models/guitar.glb"; if(t.equals("BASS"))return "models/bass.glb";
            if(t.equals("KEYS")||t.equals("PIANO"))return "models/keyboard.glb"; if(t.equals("KANUN"))return "models/kanun.glb"; if(t.equals("BAGLAMA"))return "models/baglama.glb"; if(t.equals("UD"))return "models/ud.glb";
            if(t.equals("KEMAN")||t.equals("VIOLA"))return "models/violin.glb"; if(t.equals("CELLO"))return "models/cello.glb"; if(t.equals("DARBUKA"))return "models/darbuka.glb";
            if(t.equals("NEY"))return "models/ney.glb"; if(t.equals("KLARNET"))return "models/clarinet.glb"; if(t.equals("SAKSAFON"))return "models/sax.glb"; if(t.equals("TROMPET"))return "models/trumpet.glb"; if(t.equals("TROMBON"))return "models/trombone.glb"; if(t.equals("AKORDEON"))return "models/accordion.glb"; if(t.equals("PERCUSSION"))return "models/percussion.glb";
            if(t.equals("IEM"))return "models/iem.glb"; if(t.equals("TRUSS"))return "models/truss.glb"; if(t.equals("MOVING HEAD"))return "models/movinghead.glb"; if(t.equals("FOH CONSOLE")||t.equals("MONITOR CONSOLE"))return "models/console.glb";
            if(t.equals("LAPTOP"))return "models/laptop.glb"; if(t.equals("MIC STAND"))return "models/micstand.glb"; if(t.equals("SPEAKER STAND"))return "models/speakerstand.glb"; if(t.equals("LED WALL"))return "models/ledwall.glb"; if(t.equals("RISER"))return "models/riser.glb"; if(t.equals("BARRIER"))return "models/barrier.glb"; return "models/pc.glb";
        }
    }

    class SceneRenderer extends SurfaceView {
        private final UiHelper uiHelper = new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK);
        private Engine engine; private Renderer renderer; private Scene filamentScene; private com.google.android.filament.Camera camera; private SwapChain swapChain;
        private MaterialProvider materials; private AssetLoader loader; private ResourceLoader resources; private int sun; private FilamentAsset stageAsset; private final List<Equipment> objects=new ArrayList<>();
        private Equipment selected; float stageW=12,stageD=8,stageH=1; private float yaw=35,pitch=28,distance=22,lastX,lastY,lastPinch; boolean moveMode=true; private View viewRef;
        private final Choreographer choreographer=Choreographer.getInstance(); private final Choreographer.FrameCallback frameCallback=this::renderFrame;
        private final List<String> undo=new ArrayList<>(), redo=new ArrayList<>();

        SceneRenderer(Activity context) {
            super(context); engine=Engine.create(); renderer=engine.createRenderer(); filamentScene=engine.createScene(); camera=engine.createCamera(EntityManager.get().create()); camera.setExposure(16f,1f/125f,100f);
            viewRef=engine.createView(); viewRef.setScene(filamentScene); viewRef.setCamera(camera); viewRef.setSampleCount(4); viewRef.setAntiAliasing(View.AntiAliasing.FXAA);
            materials=new UbershaderProvider(engine); loader=new AssetLoader(engine,materials,EntityManager.get()); resources=new ResourceLoader(engine);
            sun=EntityManager.get().create(); new LightManager.Builder(LightManager.Type.SUN).color(1f,.98f,.94f).intensity(110000f).direction(-.35f,-1f,-.25f).castShadows(true).build(engine,sun); filamentScene.addEntity(sun);
            stageAsset=loadAsset("models/stage.glb"); if(stageAsset!=null){filamentScene.addEntities(stageAsset.getEntities());applyStageModel();}
            uiHelper.setRenderCallback(new UiHelper.RendererCallback(){
                @Override public void onNativeWindowChanged(Surface surface){if(swapChain!=null)engine.destroySwapChain(swapChain);swapChain=engine.createSwapChain(surface);}
                @Override public void onDetachedFromSurface(){if(swapChain!=null){engine.destroySwapChain(swapChain);swapChain=null;}}
                @Override public void onResized(int width,int height){viewRef.setViewport(new Viewport(0,0,width,height));updateCamera();}
            }); uiHelper.attachTo(this); updateCamera(); choreographer.postFrameCallback(frameCallback);
        }
        private void renderFrame(long t){if(swapChain!=null&&renderer.beginFrame(swapChain,t)){resources.asyncUpdateLoad();loader.gc();renderer.render(viewRef);renderer.endFrame();}choreographer.postFrameCallback(frameCallback);}
        private void updateCamera(){double ya=Math.toRadians(yaw),pi=Math.toRadians(pitch);float ex=(float)(Math.sin(ya)*Math.cos(pi)*distance),ey=(float)(Math.sin(pi)*distance+4),ez=(float)(Math.cos(ya)*Math.cos(pi)*distance);camera.lookAt(ex,ey,ez,0,0,0,0,1,0);}
        void applyStageModel(){if(stageAsset==null)return;TransformManager.Instance inst=engine.getTransformManager().getInstance(stageAsset.getRoot());float[] m=new float[16];android.opengl.Matrix.setIdentityM(m,0);android.opengl.Matrix.translateM(m,0,0,-.5f,0);android.opengl.Matrix.scaleM(m,0,stageW,stageH,stageD);engine.getTransformManager().setTransform(inst,m);}
        void addEquipment(String type){saveUndo();Equipment e=new Equipment(type);int i=objects.size();e.x=(i%6-2.5f)*1.5f;e.z=(i/6)*1.5f-2f;e.asset=loadAsset(e.modelFile);if(e.asset==null){Toast.makeText(MainActivity.this,"3D model yok: "+e.modelFile,Toast.LENGTH_SHORT).show();return;}filamentScene.addEntities(e.asset.getEntities());objects.add(e);selected=e;applyTransform(e);syncFields(e);invalidate();}
        void addListener(){saveUndo();Equipment e=new Equipment("LISTENER");e.width=.35f;e.height=1.7f;e.depth=.35f;e.x=0;e.z=stageD/2-1;e.modelFile="models/pc.glb";e.asset=loadAsset(e.modelFile);if(e.asset==null)return;filamentScene.addEntities(e.asset.getEntities());objects.add(e);selected=e;applyTransform(e);syncFields(e);invalidate();}
        private FilamentAsset loadAsset(String path){try{InputStream in=getAssets().open(path);byte[] bytes=readAll(in);in.close();FilamentAsset a=loader.createAsset(bytes,bytes.length);if(a==null)return null;resources.loadResources(a);a.releaseSourceData();return a;}catch(Exception e){return null;}}
        void applyTransform(Equipment e){if(e==null||e.asset==null)return;TransformManager tm=engine.getTransformManager();TransformManager.Instance inst=tm.getInstance(e.asset.getRoot());float[] m=new float[16];android.opengl.Matrix.setIdentityM(m,0);android.opengl.Matrix.translateM(m,0,e.x,e.y,e.z);android.opengl.Matrix.rotateM(m,0,e.rotationX,1,0,0);android.opengl.Matrix.rotateM(m,0,e.rotationY,0,1,0);android.opengl.Matrix.rotateM(m,0,e.rotationZ,0,0,1);android.opengl.Matrix.scaleM(m,0,e.width,e.height,e.depth);tm.setTransform(inst,m);}
        Equipment selected(){return selected;}
        void rotateSelected(float d){if(selected==null)return;saveUndo();selected.rotationY+=d;applyTransform(selected);syncFields(selected);invalidate();}
        void selectNext(){if(objects.isEmpty())return;int i=objects.indexOf(selected);selected=objects.get((i+1+objects.size())%objects.size());syncFields(selected);invalidate();}
        void deleteSelected(){if(selected==null)return;saveUndo();filamentScene.removeEntities(selected.asset.getEntities());loader.destroyAsset(selected.asset);objects.remove(selected);selected=null;invalidate();}

        private Equipment pick(float sx,float sy){if(objects.isEmpty()||getWidth()<=0||getHeight()<=0)return null;double ya=Math.toRadians(yaw),pi=Math.toRadians(pitch);float ex=(float)(Math.sin(ya)*Math.cos(pi)*distance),ey=(float)(Math.sin(pi)*distance+4),ez=(float)(Math.cos(ya)*Math.cos(pi)*distance);float fx=-ex,fy=-ey,fz=-ez;float fl=(float)Math.sqrt(fx*fx+fy*fy+fz*fz);fx/=fl;fy/=fl;fz/=fl;float rx=(float)Math.cos(ya),rz=(float)-Math.sin(ya);float ux=fy*rz,uy=fz*rx-fx*rz,uz=-fy*rx;float aspect=(float)getWidth()/Math.max(1,getHeight());float tan=(float)Math.tan(Math.toRadians(55)/2);Equipment best=null;float bestD=Float.MAX_VALUE;for(Equipment e:objects){float vx=e.x-ex,vy=e.y-ey,vz=e.z-ez;float cx=vx*rx+vz*rz,cy=vx*ux+vy*uy+vz*uz,cz=vx*fx+vy*fy+vz*fz;if(cz<=.1f)continue;float px=getWidth()/2f+(cx/(cz*tan*aspect))*getWidth()/2f;float py=getHeight()/2f-(cy/(cz*tan))*getHeight()/2f;float d=(float)Math.hypot(px-sx,py-sy);float radius=Math.max(22,Math.min(100,40*e.width));if(d<radius&&d<bestD){best=e;bestD=d;}}return best;}

        String splReport(){if(selected==null)return "Önce PA ekipmanı seçin.";float lx=0,ly=1.7f,lz=Math.max(0,stageD/2-1);double dx=selected.x-lx,dy=selected.y-ly,dz=selected.z-lz,d=Math.max(1,Math.sqrt(dx*dx+dy*dy+dz*dz));double angle=Math.toDegrees(Math.atan2(Math.abs(dx),Math.max(.001,Math.abs(-dz))));double spl=Acoustics.spl(selected.sensitivity,selected.power,d,angle,selected.coverage,selected.maxSPL);return String.format(Locale.US,"%s\nModel: %s\nMesafe: %.2f m\nTahmini SPL: %.1f dB\nMax SPL: %.1f dB\nKapsama: %.0f°\nMesafe kaybı: %.1f dB\nAçı düzeltmesi: %.1f dB",selected.type,selected.modelName,d,spl,selected.maxSPL,selected.coverage,Acoustics.distanceLossDb(d),Acoustics.directivityCorrectionDb(angle,selected.coverage));}
        String heatmapReport(){if(objects.isEmpty())return "SPL heatmap için PA ekipmanı ekleyin.";double sum=0,min=999,max=-999;int n=0;for(float z=-stageD/2;z<=stageD/2;z+=1){for(float x=-stageW/2;x<=stageW/2;x+=1){double total=0;for(Equipment e:objects)if(e.category.equals("PA")){double dx=x-e.x,dy=1.7-e.y,dz=z-e.z,d=Math.max(1,Math.sqrt(dx*dx+dy*dy+dz*dz));double a=Math.toDegrees(Math.atan2(Math.abs(dx),Math.max(.001,Math.abs(-dz))));total=Math.max(total,Acoustics.spl(e.sensitivity,e.power,d,a,e.coverage,e.maxSPL));}if(total>0){sum+=total;min=Math.min(min,total);max=Math.max(max,total);n++;}}}return n==0?"PA ekipmanı yok.":String.format(Locale.US,"SPL HEATMAP\nNokta: %d\nMin: %.1f dB\nOrtalama: %.1f dB\nMax: %.1f dB",n,min,sum/n,max);}

        String exportProject(){JSONObject root=new JSONObject();try{root.put("schema","stagepulse.system-design.v2");JSONObject st=new JSONObject();st.put("width",stageW);st.put("depth",stageD);st.put("height",stageH);root.put("stage",st);JSONArray a=new JSONArray();for(Equipment e:objects){JSONObject o=new JSONObject();o.put("type",e.type);o.put("model",e.modelFile);o.put("modelName",e.modelName);o.put("category",e.category);o.put("x",e.x);o.put("y",e.y);o.put("z",e.z);o.put("rotationX",e.rotationX);o.put("rotationY",e.rotationY);o.put("rotationZ",e.rotationZ);o.put("width",e.width);o.put("height",e.height);o.put("depth",e.depth);o.put("sensitivity",e.sensitivity);o.put("power",e.power);o.put("maxSPL",e.maxSPL);o.put("coverage",e.coverage);a.put(o);}root.put("equipment",a);return root.toString(2);}catch(Exception e){return "{}";}}
        void importProject(String json)throws Exception{saveUndo();JSONObject root=new JSONObject(json);JSONObject st=root.optJSONObject("stage");if(st!=null){stageW=(float)st.optDouble("width",12);stageD=(float)st.optDouble("depth",8);stageH=(float)st.optDouble("height",1);applyStageModel();}for(Equipment e:objects)if(e.asset!=null){filamentScene.removeEntities(e.asset.getEntities());loader.destroyAsset(e.asset);}objects.clear();JSONArray a=root.optJSONArray("equipment");if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Equipment e=new Equipment(o.optString("type","PC"));e.x=(float)o.optDouble("x",0);e.y=(float)o.optDouble("y",0);e.z=(float)o.optDouble("z",0);e.rotationX=(float)o.optDouble("rotationX",0);e.rotationY=(float)o.optDouble("rotationY",0);e.rotationZ=(float)o.optDouble("rotationZ",0);e.width=(float)o.optDouble("width",e.width);e.height=(float)o.optDouble("height",e.height);e.depth=(float)o.optDouble("depth",e.depth);e.sensitivity=(float)o.optDouble("sensitivity",e.sensitivity);e.power=(float)o.optDouble("power",e.power);e.maxSPL=(float)o.optDouble("maxSPL",e.maxSPL);e.coverage=(float)o.optDouble("coverage",e.coverage);e.asset=loadAsset(e.modelFile);if(e.asset!=null){filamentScene.addEntities(e.asset.getEntities());objects.add(e);applyTransform(e);}}selected=objects.isEmpty()?null:objects.get(0);syncFields(selected);invalidate();}

        void saveUndo(){if(undo.size()>20)undo.remove(0);undo.add(exportProject());redo.clear();}
        void undo(){if(undo.isEmpty())return;try{String current=exportProject();redo.add(current);String previous=undo.remove(undo.size()-1);importProjectWithoutHistory(previous);}catch(Exception ignored){}}
        void redo(){if(redo.isEmpty())return;try{String current=exportProject();undo.add(current);String next=redo.remove(redo.size()-1);importProjectWithoutHistory(next);}catch(Exception ignored){}}
        private void importProjectWithoutHistory(String json)throws Exception{JSONObject root=new JSONObject(json);JSONObject st=root.optJSONObject("stage");if(st!=null){stageW=(float)st.optDouble("width",stageW);stageD=(float)st.optDouble("depth",stageD);stageH=(float)st.optDouble("height",stageH);applyStageModel();}for(Equipment e:objects)if(e.asset!=null){filamentScene.removeEntities(e.asset.getEntities());loader.destroyAsset(e.asset);}objects.clear();JSONArray a=root.optJSONArray("equipment");if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Equipment e=new Equipment(o.optString("type","PC"));e.x=(float)o.optDouble("x",0);e.y=(float)o.optDouble("y",0);e.z=(float)o.optDouble("z",0);e.rotationX=(float)o.optDouble("rotationX",0);e.rotationY=(float)o.optDouble("rotationY",0);e.rotationZ=(float)o.optDouble("rotationZ",0);e.asset=loadAsset(e.modelFile);if(e.asset!=null){filamentScene.addEntities(e.asset.getEntities());objects.add(e);applyTransform(e);}}selected=objects.isEmpty()?null:objects.get(0);syncFields(selected);invalidate();}

        byte[] buildRiderPdf()throws IOException{PdfDocument doc=new PdfDocument();PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(1190,842,1).create();PdfDocument.Page page=doc.startPage(info);android.graphics.Canvas c=page.getCanvas();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setTextSize(22);c.drawText("STAGEPULSE — SYSTEM DESIGN / RIDER",40,42,p);p.setTextSize(14);c.drawText(String.format(Locale.US,"Stage %.2f m × %.2f m × %.2f m",stageW,stageD,stageH),40,68,p);p.setTextSize(11);c.drawText("Stage Plot • Equipment Coordinates • SPL Reference",40,90,p);float sx=60,sy=120,scale=Math.min(720/stageW,500/stageD);p.setStyle(Paint.Style.STROKE);c.drawRect(sx,sy,sx+stageW*scale,sy+stageD*scale,p);p.setStyle(Paint.Style.FILL);for(Equipment e:objects){float px=sx+(e.x+stageW/2)*scale,py=sy+(e.z+stageD/2)*scale;p.setColor(e.category.equals("PA")?Color.rgb(30,100,180):Color.rgb(80,80,80));c.drawCircle(px,py,6,p);p.setColor(Color.BLACK);c.drawText(e.type,px+8,py+4,p);}p.setTextSize(10);float yy=660;c.drawText("Equipment",820,120,p);int row=0;for(Equipment e:objects){if(row>30)break;c.drawText(String.format(Locale.US,"%s  X %.2f Y %.2f Z %.2f",e.type,e.x,e.y,e.z),820,145+row*18,p);row++;}c.drawText("Acoustic result is a design estimate; verify exact manufacturer datasheet and field measurement before deployment.",40,780,p);doc.finishPage(page);ByteArrayOutputStream out=new ByteArrayOutputStream();doc.writeTo(out);doc.close();return out.toByteArray();}

        @Override public boolean onTouchEvent(MotionEvent ev){switch(ev.getActionMasked()){case MotionEvent.ACTION_DOWN:lastX=ev.getX();lastY=ev.getY();if(moveMode){Equipment hit=pick(lastX,lastY);if(hit!=null){selected=hit;syncFields(selected);saveUndo();}}return true;case MotionEvent.ACTION_POINTER_DOWN:if(ev.getPointerCount()>=2)lastPinch=pinch(ev);return true;case MotionEvent.ACTION_MOVE:if(ev.getPointerCount()>=2){float d=pinch(ev);if(lastPinch>0){distance+=(lastPinch-d)*.02f;distance=Math.max(5,Math.min(80,distance));}lastPinch=d;}else if(moveMode&&selected!=null){float dx=ev.getX()-lastX,dy=ev.getY()-lastY;selected.x+=dx*.018f;selected.z-=dy*.018f;selected.x=Math.max(-stageW/2,Math.min(stageW/2,selected.x));selected.z=Math.max(-stageD/2,Math.min(stageD/2,selected.z));applyTransform(selected);syncFields(selected);}else{yaw+=(ev.getX()-lastX)*.35f;pitch+=(ev.getY()-lastY)*.25f;pitch=Math.max(-80,Math.min(80,pitch));updateCamera();}lastX=ev.getX();lastY=ev.getY();invalidate();return true;case MotionEvent.ACTION_UP:performClick();return true;}return true;}
        private float pinch(MotionEvent e){if(e.getPointerCount()<2)return 0;float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1);return (float)Math.sqrt(dx*dx+dy*dy);}
        @Override public boolean performClick(){super.performClick();return true;}
        private byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
        @Override protected void onDetachedFromWindow(){choreographer.removeFrameCallback(frameCallback);if(swapChain!=null)engine.destroySwapChain(swapChain);if(sun!=0)engine.destroyEntity(sun);for(Equipment e:objects)if(e.asset!=null)loader.destroyAsset(e.asset);if(stageAsset!=null)loader.destroyAsset(stageAsset);resources.destroy();loader.destroy();materials.destroyMaterials();materials.destroy();engine.destroyView(viewRef);engine.destroyCameraComponent(camera.getEntity());engine.destroyRenderer(renderer);engine.destroyScene(filamentScene);engine.destroy();super.onDetachedFromWindow();}
    }
}
