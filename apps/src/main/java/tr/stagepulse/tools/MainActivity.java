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
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.filament.Camera;
import com.google.android.filament.Engine;
import com.google.android.filament.Filament;
import com.google.android.filament.LightManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Scene;
import com.google.android.filament.SwapChain;
import com.google.android.filament.View;
import com.google.android.filament.Viewport;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.UbershaderProvider;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.EntityManager;
import com.google.android.filament.TransformManager;

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

    private SceneRenderer scene;
    private TextView status;
    private EditText stageW, stageD, stageH, xField, yField, zField, rotField;
    private byte[] pendingDocument;
    private String pendingMime;

    private final String[] LIBRARY = {
            "LINE ARRAY 4", "LINE ARRAY 8", "SUB DOUBLE 18", "SUB SINGLE 18", "MONITOR 12", "MONITOR 15",
            "DRUM KIT", "GUITAR", "BASS", "KEYS", "KANUN", "BAGLAMA", "UD", "KEMAN", "CELLO", "DARBUKA", "NEY", "KLARNET", "SAKSAFON", "TROMPET", "TROMBON", "AKORDEON", "PERCUSSION", "IEM", "TRUSS", "MOVING HEAD", "FOH CONSOLE",
            "LAPTOP", "MIC STAND", "SPEAKER STAND", "LED WALL", "RISER", "BARRIER", "PC"
    };

    static {
        Filament.init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        Button rider = button("RIDER PDF");
        rider.setOnClickListener(v -> createRiderPdf());
        header.addView(rider, new LinearLayout.LayoutParams(110, 58));

        Button project = button("PROJECT");
        project.setOnClickListener(v -> createProjectJson());
        header.addView(project, new LinearLayout.LayoutParams(105, 58));
        root.addView(header);

        HorizontalScrollView libraryScroll = new HorizontalScrollView(this);
        LinearLayout library = new LinearLayout(this);
        library.setOrientation(LinearLayout.HORIZONTAL);
        for (String item : LIBRARY) {
            Button b = button(item);
            b.setTextSize(9);
            b.setOnClickListener(v -> scene.addEquipment(item));
            library.addView(b, new LinearLayout.LayoutParams(118, 54));
        }
        libraryScroll.addView(library);
        root.addView(libraryScroll, new LinearLayout.LayoutParams(-1, 58));

        scene = new SceneRenderer(this);
        root.addView(scene, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout inspector = new LinearLayout(this);
        inspector.setOrientation(LinearLayout.VERTICAL);
        inspector.setPadding(8, 4, 8, 4);
        inspector.setBackgroundColor(Color.rgb(17, 22, 29));

        LinearLayout stageRow = new LinearLayout(this);
        stageW = field("STAGE W", "12");
        stageD = field("STAGE D", "8");
        stageH = field("STAGE H", "1");
        stageRow.addView(stageW, new LinearLayout.LayoutParams(100, 46));
        stageRow.addView(stageD, new LinearLayout.LayoutParams(100, 46));
        stageRow.addView(stageH, new LinearLayout.LayoutParams(100, 46));
        Button applyStage = button("APPLY STAGE");
        applyStage.setOnClickListener(v -> applyStage());
        stageRow.addView(applyStage, new LinearLayout.LayoutParams(125, 46));
        Button listener = button("LISTENER");
        listener.setOnClickListener(v -> scene.addListener());
        stageRow.addView(listener, new LinearLayout.LayoutParams(100, 46));
        inspector.addView(stageRow);

        LinearLayout transformRow = new LinearLayout(this);
        xField = field("X", "0");
        yField = field("Y", "0");
        zField = field("Z", "0");
        rotField = field("ROT Y", "0");
        transformRow.addView(xField, new LinearLayout.LayoutParams(90, 46));
        transformRow.addView(yField, new LinearLayout.LayoutParams(90, 46));
        transformRow.addView(zField, new LinearLayout.LayoutParams(90, 46));
        transformRow.addView(rotField, new LinearLayout.LayoutParams(90, 46));
        Button apply = button("APPLY XYZ");
        apply.setOnClickListener(v -> applyObject());
        transformRow.addView(apply, new LinearLayout.LayoutParams(110, 46));
        Button rotL = button("↺");
        rotL.setOnClickListener(v -> scene.rotateSelected(-5));
        transformRow.addView(rotL, new LinearLayout.LayoutParams(60, 46));
        Button rotR = button("↻");
        rotR.setOnClickListener(v -> scene.rotateSelected(5));
        transformRow.addView(rotR, new LinearLayout.LayoutParams(60, 46));
        Button next = button("NEXT");
        next.setOnClickListener(v -> scene.selectNext());
        transformRow.addView(next, new LinearLayout.LayoutParams(80, 46));
        Button mode = button("MOVE/CAM");
        mode.setOnClickListener(v -> { scene.moveMode = !scene.moveMode; mode.setText(scene.moveMode ? "MOVE" : "CAMERA"); });
        transformRow.addView(mode, new LinearLayout.LayoutParams(100, 46));
        Button del = button("DELETE");
        del.setOnClickListener(v -> scene.deleteSelected());
        transformRow.addView(del, new LinearLayout.LayoutParams(90, 46));
        Button spl = button("SPL / COVERAGE");
        spl.setOnClickListener(v -> status.setText(scene.splReport()));
        transformRow.addView(spl, new LinearLayout.LayoutParams(145, 46));
        inspector.addView(transformRow);

        status = text("Ekipman ekleyin • seçili ekipmanı sürükleyin • iki parmakla kamera zoom/orbit", 12, Color.rgb(205, 214, 224));
        inspector.addView(status, new LinearLayout.LayoutParams(-1, 42));
        root.addView(inspector, new LinearLayout.LayoutParams(-1, 142));

        setContentView(root);
    }

    private void applyStage() {
        scene.stageW = number(stageW, 12);
        scene.stageD = number(stageD, 8);
        scene.stageH = number(stageH, 1);
        scene.applyStageModel();
        scene.invalidate();
    }

    private void applyObject() {
        Equipment e = scene.selected();
        if (e == null) {
            status.setText("Önce ekipman ekleyin/seçin.");
            return;
        }
        e.x = number(xField, e.x);
        e.y = number(yField, e.y);
        e.z = number(zField, e.z);
        e.rotationY = number(rotField, e.rotationY);
        scene.applyTransform(e);
        scene.invalidate();
    }

    private void syncFields(Equipment e) {
        if (e == null) return;
        xField.setText(fmt(e.x));
        yField.setText(fmt(e.y));
        zField.setText(fmt(e.z));
        rotField.setText(fmt(e.rotationY));
    }

    private void createRiderPdf() {
        try {
            pendingDocument = scene.buildRiderPdf();
            pendingMime = "application/pdf";
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.setType(pendingMime);
            i.putExtra(Intent.EXTRA_TITLE, "StagePulse-Rider.pdf");
            startActivityForResult(i, CREATE_RIDER);
        } catch (Exception e) {
            status.setText("Rider PDF oluşturulamadı: " + e.getMessage());
        }
    }

    private void createProjectJson() {
        pendingDocument = scene.exportProject().getBytes(StandardCharsets.UTF_8);
        pendingMime = "application/json";
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType(pendingMime);
        i.putExtra(Intent.EXTRA_TITLE, "StagePulse-project.json");
        startActivityForResult(i, CREATE_PROJECT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingDocument != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                out.write(pendingDocument);
                out.flush();
                status.setText("Dosya kaydedildi.");
            } catch (Exception e) {
                status.setText("Dosya yazılamadı: " + e.getMessage());
            }
        }
        pendingDocument = null;
    }

    static String fmt(float v) { return String.format(Locale.US, "%.2f", v); }
    static float number(EditText e, float fallback) {
        try { return Float.parseFloat(e.getText().toString().replace(',', '.')); }
        catch (Exception ex) { return fallback; }
    }
    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); t.setPadding(8, 0, 8, 0);
        return t;
    }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setAllCaps(false); return b;
    }
    private EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setText(value); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setTextSize(11); e.setSingleLine(true);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        return e;
    }

    public static class Equipment {
        String type, modelFile;
        float x, y, z, rotationY;
        float width, height, depth;
        float sensitivity, power, maxSPL, coverage;
        FilamentAsset asset;

        Equipment(String type) {
            this.type = type;
            this.modelFile = modelFor(type);
            configure(type);
        }

        private void configure(String t) {
            sensitivity = 96; power = 500; maxSPL = 125; coverage = 90;
            if (t.startsWith("LINE ARRAY")) { width=1f;height=.35f;depth=.65f;sensitivity=100;power=1600;maxSPL=142;coverage=90;y=5; }
            else if (t.equals("SUB DOUBLE 18")) { width=1.2f;height=1f;depth=1f;sensitivity=99;power=2400;maxSPL=139;coverage=120; }
            else if (t.equals("SUB SINGLE 18")) { width=1f;height=1f;depth=.85f;sensitivity=98;power=1200;maxSPL=136;coverage=120; }
            else if (t.equals("MONITOR 12")) { width=.75f;height=.35f;depth=.9f;sensitivity=96;power=700;maxSPL=130;coverage=80; }
            else if (t.equals("MONITOR 15")) { width=.9f;height=.42f;depth=1.05f;sensitivity=97;power=900;maxSPL=132;coverage=80; }
            else if (t.equals("DRUM KIT")) { width=2.1f;height=1.5f;depth=1.8f; }
            else if (t.equals("GUITAR")) { width=.45f;height=1.2f;depth=.35f; }
            else if (t.equals("BASS")) { width=.45f;height=1.3f;depth=.35f; }
            else if (t.equals("KEYS")) { width=1.5f;height=.45f;depth=.5f; }
            else if (t.equals("KANUN")) { width=1.1f;height=.28f;depth=.55f; }
            else if (t.equals("KEMAN")) { width=.25f;height=.9f;depth=.2f; }
            else if (t.equals("BAGLAMA")||t.equals("UD")) { width=.45f;height=1.1f;depth=.32f; }
            else if (t.equals("CELLO")) { width=.5f;height=1.25f;depth=.35f; }
            else if (t.equals("DARBUKA")) { width=.6f;height=.7f;depth=.6f; }
            else if (t.equals("NEY")||t.equals("KLARNET")) { width=.15f;height=1.1f;depth=.15f; }
            else if (t.equals("SAKSAFON")) { width=.5f;height=1.0f;depth=.35f; }
            else if (t.equals("TROMPET")||t.equals("TROMBON")) { width=.5f;height=1.0f;depth=.3f; }
            else if (t.equals("AKORDEON")) { width=.6f;height=.8f;depth=.3f; }
            else if (t.equals("PERCUSSION")) { width=1.0f;height=.8f;depth=.7f; }
            else if (t.equals("IEM")) { width=.3f;height=.12f;depth=.18f; }
            else if (t.equals("TRUSS")) { width=4f;height=.12f;depth=.12f;y=4; }
            else if (t.equals("MOVING HEAD")) { width=.45f;height=.6f;depth=.35f;y=4; }
            else if (t.equals("FOH CONSOLE")) { width=1.8f;height=.45f;depth=.85f; }
            else if (t.equals("LAPTOP")) { width=.55f;height=.35f;depth=.4f; }
            else if (t.equals("MIC STAND")) { width=.2f;height=1.7f;depth=.2f; }
            else if (t.equals("SPEAKER STAND")) { width=.35f;height=2.3f;depth=.35f;y=2.3f; }
            else if (t.equals("LED WALL")) { width=5f;height=3f;depth=.2f;y=1.5f; }
            else if (t.equals("RISER")) { width=3f;height=.5f;depth=2f; }
            else if (t.equals("BARRIER")) { width=2f;height=1.1f;depth=.12f; }
            else if (t.equals("PC")) { width=.5f;height=.35f;depth=.3f; }
        }

        static String modelFor(String t) {
            if (t.equals("LINE ARRAY 4")) return "models/linearray4.glb";
            if (t.equals("LINE ARRAY 8")) return "models/linearray8.glb";
            if (t.equals("LINE ARRAY")) return "models/linearray.glb";
            if (t.equals("SUB DOUBLE 18")) return "models/sub_double18.glb";
            if (t.equals("SUB SINGLE 18")) return "models/sub_single18.glb";
            if (t.equals("MONITOR 12")) return "models/monitor12.glb";
            if (t.equals("MONITOR 15")) return "models/monitor15.glb";
            if (t.equals("DRUM KIT")) return "models/drumkit.glb";
            if (t.equals("GUITAR")) return "models/guitar.glb";
            if (t.equals("BASS")) return "models/bass.glb";
            if (t.equals("KEYS")) return "models/keyboard.glb";
            if (t.equals("KANUN")) return "models/kanun.glb";
            if (t.equals("KEMAN")) return "models/violin.glb";
            if (t.equals("BAGLAMA")) return "models/baglama.glb";
            if (t.equals("UD")) return "models/ud.glb";
            if (t.equals("CELLO")) return "models/cello.glb";
            if (t.equals("DARBUKA")) return "models/darbuka.glb";
            if (t.equals("NEY")) return "models/ney.glb";
            if (t.equals("KLARNET")) return "models/clarinet.glb";
            if (t.equals("SAKSAFON")) return "models/sax.glb";
            if (t.equals("TROMPET")) return "models/trumpet.glb";
            if (t.equals("TROMBON")) return "models/trombone.glb";
            if (t.equals("AKORDEON")) return "models/accordion.glb";
            if (t.equals("PERCUSSION")) return "models/percussion.glb";
            if (t.equals("IEM")) return "models/iem.glb";
            if (t.equals("TRUSS")) return "models/truss.glb";
            if (t.equals("MOVING HEAD")) return "models/movinghead.glb";
            if (t.equals("FOH CONSOLE")) return "models/console.glb";
            if (t.equals("LAPTOP")) return "models/laptop.glb";
            if (t.equals("MIC STAND")) return "models/micstand.glb";
            if (t.equals("SPEAKER STAND")) return "models/speakerstand.glb";
            if (t.equals("LED WALL")) return "models/ledwall.glb";
            if (t.equals("RISER")) return "models/riser.glb";
            if (t.equals("BARRIER")) return "models/barrier.glb";
            return "models/pc.glb";
        }
    }

    class SceneRenderer extends SurfaceView {
        private final UiHelper uiHelper = new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK);
        private Engine engine;
        private Renderer renderer;
        private Scene filamentScene;
        private Camera camera;
        private SwapChain swapChain;
        private MaterialProvider materials;
        private AssetLoader loader;
        private ResourceLoader resources;
        private int sun;
        private FilamentAsset stageAsset;
        private final List<Equipment> objects = new ArrayList<>();
        private Equipment selected;
        float stageW=12, stageD=8, stageH=1;
        private float yaw=35, pitch=28, distance=22;
        private float lastX,lastY,lastPinch;
        private boolean moveMode = true;
        private final Choreographer choreographer = Choreographer.getInstance();
        private final Choreographer.FrameCallback frameCallback = this::renderFrame;

        SceneRenderer(Activity context) {
            super(context);
            engine = Engine.create();
            renderer = engine.createRenderer();
            filamentScene = engine.createScene();
            camera = engine.createCamera(EntityManager.get().create());
            camera.setExposure(16f, 1f/125f, 100f);
            View view = engine.createView();
            view.setScene(filamentScene);
            view.setCamera(camera);
            view.setSampleCount(4);
            view.setAntiAliasing(View.AntiAliasing.FXAA);
            setTag(view);

            materials = new UbershaderProvider(engine);
            loader = new AssetLoader(engine, materials, EntityManager.get());
            resources = new ResourceLoader(engine);

            sun = EntityManager.get().create();
            new LightManager.Builder(LightManager.Type.SUN)
                    .color(1f, 0.98f, 0.94f)
                    .intensity(110000f)
                    .direction(-0.35f, -1f, -0.25f)
                    .castShadows(true)
                    .build(engine, sun);
            filamentScene.addEntity(sun);
            stageAsset = loadAsset("models/stage.glb");
            if (stageAsset != null) { filamentScene.addEntities(stageAsset.getEntities()); applyStageModel(); }

            uiHelper.setRenderCallback(new UiHelper.RendererCallback() {
                @Override public void onNativeWindowChanged(android.view.Surface surface) {
                    if (swapChain != null) engine.destroySwapChain(swapChain);
                    swapChain = engine.createSwapChain(surface);
                }
                @Override public void onDetachedFromSurface() {
                    if (swapChain != null) { engine.destroySwapChain(swapChain); swapChain=null; }
                }
                @Override public void onResized(int width, int height) {
                    view.setViewport(new Viewport(0,0,width,height));
                    updateCamera();
                }
            });
            uiHelper.attachTo(this);
            updateCamera();
            choreographer.postFrameCallback(frameCallback);
        }

        private void setTag(View view) { setContentDescription("StagePulse 3D Scene"); this.setTag(view, view); }
        private void setTag(View view, Object ignored) { this.viewRef = view; }
        private View viewRef;

        private void renderFrame(long frameTimeNanos) {
            if (swapChain != null && renderer.beginFrame(swapChain, frameTimeNanos)) {
                resources.asyncUpdateLoad();
                loader.gc();
                renderer.render(viewRef);
                renderer.endFrame();
            }
            choreographer.postFrameCallback(frameCallback);
        }

        private void updateCamera() {
            if (camera == null) return;
            double ya=Math.toRadians(yaw), pi=Math.toRadians(pitch);
            float ex=(float)(Math.sin(ya)*Math.cos(pi)*distance);
            float ey=(float)(Math.sin(pi)*distance+4);
            float ez=(float)(Math.cos(ya)*Math.cos(pi)*distance);
            camera.lookAt(ex,ey,ez,0,0,0,0,1,0);
        }

        private void applyStageModel() {
            if (stageAsset == null) return;
            TransformManager.Instance inst = engine.getTransformManager().getInstance(stageAsset.getRoot());
            float[] m = new float[16]; android.opengl.Matrix.setIdentityM(m,0);
            android.opengl.Matrix.translateM(m,0,0,-0.5f,0);
            android.opengl.Matrix.scaleM(m,0,stageW,stageH,stageD);
            engine.getTransformManager().setTransform(inst,m);
        }

        void addEquipment(String type) {
            Equipment e = new Equipment(type);
            int i=objects.size();
            e.x=(i%6-2.5f)*1.5f;
            e.z=(i/6)*1.5f-2f;
            e.asset=loadAsset(e.modelFile);
            if (e.asset == null) { Toast.makeText(MainActivity.this,"3D model yüklenemedi: "+e.modelFile,Toast.LENGTH_SHORT).show(); return; }
            filamentScene.addEntities(e.asset.getEntities());
            objects.add(e); selected=e; applyTransform(e); syncFields(e); invalidate();
        }

        void addListener() {
            Equipment e=new Equipment("LISTENER"); e.width=.35f;e.height=1.7f;e.depth=.35f;e.x=0;e.z=4;e.modelFile="models/pc.glb";
            e.asset=loadAsset(e.modelFile); if(e.asset==null)return; filamentScene.addEntities(e.asset.getEntities()); objects.add(e); selected=e; applyTransform(e); syncFields(e); invalidate();
        }

        private FilamentAsset loadAsset(String path) {
            try {
                InputStream in=getAssets().open(path); byte[] bytes=readAll(in); in.close();
                FilamentAsset asset=loader.createAsset(bytes, bytes.length);
                if(asset==null)return null;
                resources.loadResources(asset);
                asset.releaseSourceData();
                return asset;
            } catch(Exception e) { return null; }
        }

        void applyTransform(Equipment e) {
            if(e==null||e.asset==null)return;
            TransformManager tm=engine.getTransformManager();
            TransformManager.Instance inst=tm.getInstance(e.asset.getRoot());
            float[] m=new float[16]; android.opengl.Matrix.setIdentityM(m,0);
            android.opengl.Matrix.translateM(m,0,e.x,e.y,e.z);
            android.opengl.Matrix.rotateM(m,0,e.rotationY,0,1,0);
            android.opengl.Matrix.scaleM(m,0,e.width,e.height,e.depth);
            tm.setTransform(inst,m);
        }

        Equipment selected(){return selected;}

        void rotateSelected(float deg){if(selected==null)return;selected.rotationY+=deg;applyTransform(selected);syncFields(selected);invalidate();}
        void selectNext(){if(objects.isEmpty())return;int i=objects.indexOf(selected);selected=objects.get((i+1+objects.size())%objects.size());syncFields(selected);invalidate();}
        void deleteSelected(){if(selected==null)return;filamentScene.removeEntities(selected.asset.getEntities());loader.destroyAsset(selected.asset);objects.remove(selected);selected=null;invalidate();}

        String splReport(){
            if(selected==null)return "Önce PA ekipmanı seçin.";
            float lx=0,ly=1.7f,lz=Math.max(0,stageD/2-1);
            double dx=selected.x-lx,dy=selected.y-ly,dz=selected.z-lz;
            double d=Math.max(1,Math.sqrt(dx*dx+dy*dy+dz*dz));
            double angle=Math.toDegrees(Math.atan2(Math.abs(dx),Math.max(0.001,Math.abs(-dz))));
            double powerGain=Acoustics.powerGainDb(selected.power);
            double distanceLoss=Acoustics.distanceLossDb(d);
            double directivity=Acoustics.directivityCorrectionDb(angle,selected.coverage);
            double spl=Acoustics.spl(selected.sensitivity,selected.power,d,angle,selected.coverage,selected.maxSPL);
            double maxDistance=Math.pow(10,(selected.sensitivity+powerGain-selected.maxSPL)/20.0);
            return String.format(Locale.US,"%s\nMesafe: %.2f m\nTahmini SPL: %.1f dB\nMax SPL sınırı: %.1f dB\nKapsama: %.0f°\n1/r kaybı: %.1f dB\nAçı düzeltmesi: %.1f dB\nTeorik max-SPL mesafesi: %.1f m",selected.type,d,spl,selected.maxSPL,selected.coverage,distanceLoss,directivity,maxDistance);
        }

        private double directionAttenuation(Equipment e,double dx,double dz){
            if(e.coverage<=0)return -12;
            double forward=-dz;
            double angle=Math.toDegrees(Math.atan2(Math.abs(dx),Math.max(0.001,Math.abs(forward))));
            if(angle<=e.coverage/2)return 0;
            double over=angle-e.coverage/2;
            return -Math.min(18,over*0.6);
        }

        String exportProject(){
            StringBuilder s=new StringBuilder();
            s.append("{\n  \"schema\":\"stagepulse.system-design.v1\",\n");
            s.append(String.format(Locale.US,"  \"stage\":{\"width\":%.2f,\"depth\":%.2f,\"height\":%.2f},\n",stageW,stageD,stageH));
            s.append("  \"equipment\":[\n");
            for(int i=0;i<objects.size();i++){
                Equipment e=objects.get(i);
                s.append(String.format(Locale.US,"    {\"type\":\"%s\",\"model\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"rotationY\":%.2f,\"sensitivity\":%.1f,\"power\":%.1f,\"maxSPL\":%.1f,\"coverage\":%.1f}%s\n",e.type,e.modelFile,e.x,e.y,e.z,e.rotationY,e.sensitivity,e.power,e.maxSPL,e.coverage,i+1==objects.size()?"":","));
            }
            s.append("  ]\n}\n"); return s.toString();
        }

        byte[] buildRiderPdf() throws IOException {
            PdfDocument doc=new PdfDocument();
            PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(1190,842,1).create();
            PdfDocument.Page page=doc.startPage(info); android.graphics.Canvas c=page.getCanvas();
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(Color.BLACK); p.setTextSize(22); c.drawText("STAGEPULSE — SYSTEM DESIGN / RIDER",40,42,p);
            p.setTextSize(14); c.drawText(String.format(Locale.US,"Stage %.2f m × %.2f m × %.2f m",stageW,stageD,stageH),40,68,p);
            p.setTextSize(12); c.drawText("Equipment / coordinates / acoustic reference",40,92,p);
            float sx=70,sy=130,scale=Math.min(700/stageW,520/stageD); p.setStyle(Paint.Style.STROKE); c.drawRect(sx,sy,sx+stageW*scale,sy+stageD*scale,p); p.setStyle(Paint.Style.FILL);
            for(Equipment e:objects){float px=sx+(e.x+stageW/2)*scale;float py=sy+(e.z+stageD/2)*scale;p.setColor(Color.rgb(30,110,180));c.drawCircle(px,py,7,p);p.setColor(Color.BLACK);c.drawText(e.type,px+9,py+4,p);}
            float y=700; p.setColor(Color.BLACK); p.setTextSize(11); c.drawText("SPL model: Lp = sensitivity + 10 log10(P) − 20 log10(r) + directivity correction; capped at Max SPL.",40,y,p); c.drawText("Default acoustic parameters are editable StagePulse reference values; verify exact cabinet datasheet before deployment.",40,y+18,p);
            doc.finishPage(page);
            ByteArrayOutputStream out=new ByteArrayOutputStream(); doc.writeTo(out); doc.close(); return out.toByteArray();
        }

        @Override public boolean onTouchEvent(MotionEvent ev){
            switch(ev.getActionMasked()){
                case MotionEvent.ACTION_DOWN: lastX=ev.getX();lastY=ev.getY();return true;
                case MotionEvent.ACTION_POINTER_DOWN: if(ev.getPointerCount()>=2)lastPinch=pinch(ev);return true;
                case MotionEvent.ACTION_MOVE:
                    if(ev.getPointerCount()>=2){float d=pinch(ev);if(lastPinch>0){distance+=(lastPinch-d)*.02f;distance=Math.max(6,Math.min(70,distance));}lastPinch=d;}
                    else if(moveMode && selected!=null){float dx=ev.getX()-lastX,dy=ev.getY()-lastY;selected.x+=dx*.018f;selected.z-=dy*.018f;applyTransform(selected);syncFields(selected);}
                    else{yaw+=(ev.getX()-lastX)*.35f;pitch+=(ev.getY()-lastY)*.25f;pitch=Math.max(-80,Math.min(80,pitch));updateCamera();}
                    lastX=ev.getX();lastY=ev.getY();invalidate();return true;
                case MotionEvent.ACTION_UP:performClick();return true;
            }
            return true;
        }
        private float pinch(MotionEvent e){if(e.getPointerCount()<2)return 0;float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1);return (float)Math.sqrt(dx*dx+dy*dy);}
        @Override public boolean performClick(){super.performClick();return true;}
        private byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}

        @Override protected void onDetachedFromWindow(){choreographer.removeFrameCallback(frameCallback);if(swapChain!=null)engine.destroySwapChain(swapChain);if(sun!=0)engine.destroyEntity(sun);for(Equipment e:objects)if(e.asset!=null)loader.destroyAsset(e.asset);if(stageAsset!=null)loader.destroyAsset(stageAsset);resources.destroy();loader.destroy();materials.destroyMaterials();materials.destroy();engine.destroyView(viewRef);engine.destroyCameraComponent(camera.getEntity());engine.destroyRenderer(renderer);engine.destroyScene(filamentScene);engine.destroy();super.onDetachedFromWindow();}
    }
}
