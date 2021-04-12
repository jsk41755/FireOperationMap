package com.example.fireoperationmap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private CustomAdapter adapter;
    private RecyclerView recyclerView;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private PhotoView photoView;
    private EditText searchField;
    private Map<Integer, Map<Integer, Place>> sectionData = new HashMap<>();
    private List<Arcade> arcadeList = new ArrayList<>();
    private List<Approach> approachList = new ArrayList<>();
    private List<Auto_Arcade> auto_arcadeList = new ArrayList<>();
    private List<Fireplug> fireplugList = new ArrayList<>();
    private final float slidingPanelAnchorPoint = 0.4f;
    private final PointF curRatio = new PointF(0.0f, 0.0f);
    private long backBtnTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //액션바 숨기기
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();

        searchField = findViewById(R.id.searchField);
        slidingUpPanelLayout = findViewById(R.id.slidingLayout);
        slidingUpPanelLayout.setAnchorPoint(slidingPanelAnchorPoint);
        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.ANCHORED)
                    searchField.clearFocus();
            }
        });

        initializeAdapterAndRecyclerView();
        createSearchView();
    }

    @Override
    public void onBackPressed() {//뒤로가기 두 번 눌를 시 종료
        long curTime = System.currentTimeMillis();
        long gaptime = curTime - backBtnTime;

        if (0 <= gaptime && 2000 >= gaptime) {
            // 태스크를 백그라운드로 이동
            moveTaskToBack(true);
            finishAndRemoveTask();
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            backBtnTime = curTime;
            Toast.makeText(this, "한번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show();
        }
        //super.onBackPressed();
    }

    //CustomAdapter 와 연결, 버튼과 DB 연결해주는 메써드
    private void initializeAdapterAndRecyclerView() {
        DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference("Data");
        adapter = new CustomAdapter();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(layoutManager);

        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot userSnapshot = snapshot.child("User");
                DataSnapshot sectionSnapshot = snapshot.child("Section");
                DataSnapshot arcadeSnapshot = snapshot.child("Arcade");
                DataSnapshot approachSnapshot = snapshot.child("Approach");
                DataSnapshot autoArcadeSnapshot = snapshot.child("AutoArcade");
                DataSnapshot fireplugSnapshot = snapshot.child("Fireplug");

                if (!snapshot.hasChildren())
                    Toast.makeText(MainActivity.this, "체크용", Toast.LENGTH_SHORT).show();

                adapter.init();
                for (DataSnapshot data : userSnapshot.getChildren()) {
                    User user = data.getValue(User.class);
                    adapter.addUser(user);
                }
                recyclerView.setAdapter(adapter);

                sectionData = new HashMap<>();
                for (DataSnapshot sectionData : sectionSnapshot.getChildren()) {
                    int sectionKey = Integer.parseInt(sectionData.getKey().split("_")[1]);
                    if (!MainActivity.this.sectionData.containsKey(sectionKey)) {
                        MainActivity.this.sectionData.put(sectionKey, new HashMap<>());
                    }
                    for (DataSnapshot placeData : sectionData.getChildren()) {
                        Place place = placeData.getValue(Place.class);
                        int placeKey = Integer.parseInt(placeData.getKey().split("_")[1]);
                        MainActivity.this.sectionData.get(sectionKey).put(placeKey, place);
                    }
                }

                arcadeList = new ArrayList<>();
                for (DataSnapshot data : arcadeSnapshot.getChildren()) {
                    Arcade arcade = data.getValue(Arcade.class);
                    arcadeList.add(arcade);
                }

                approachList = new ArrayList<>();
                for (DataSnapshot data : approachSnapshot.getChildren()) {
                    Approach approach = data.getValue(Approach.class);
                    approachList.add(approach);
                }

                auto_arcadeList = new ArrayList<>();
                for (DataSnapshot data : autoArcadeSnapshot.getChildren()) {
                    Auto_Arcade auto_arcade = data.getValue(Auto_Arcade.class);
                    auto_arcadeList.add(auto_arcade);
                }

                fireplugList = new ArrayList<>();
                for (DataSnapshot data : fireplugSnapshot.getChildren()) {
                    Fireplug fireplug = data.getValue(Fireplug.class);
                    fireplugList.add(fireplug);
                }

                createMapView();
                Toast.makeText(MainActivity.this, "로딩 완료", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //아이템 클릭시 이벤트 설정
        adapter.setOnSimpleItemClickListener((view, position) -> {
            User user = adapter.getItem(position);
            int sectionNum = Integer.parseInt(user.getId().split("-")[0]);
            int placeNum = Integer.parseInt(user.getId().split("-")[1]);

            //테스트용 토스트 메세지 출력
            Toast.makeText(getApplicationContext(), user.getId() + "가 선택됨 ", Toast.LENGTH_SHORT).show();

            //아이템 클릭시 검색창 비활성화
            searchField.clearFocus();

            //비율 좌표 가져오기
            curRatio.set(sectionData.get(sectionNum).get(placeNum).getX(), sectionData.get(sectionNum).get(placeNum).getY());

            ImageView icon = findViewById(R.id.pin);
            icon.setVisibility(View.VISIBLE);

            photoView.setScale(photoView.getMaximumScale(), 0.0f, 0.0f, false);
            Matrix suppMatrix = new Matrix();
            float[] values = new float[9];
            photoView.getSuppMatrix(suppMatrix);
            suppMatrix.getValues(values);

            PointF middleRatio = new PointF(0.5f, (1.0f - slidingPanelAnchorPoint) / 2.0f);
            PointF middleP = new PointF(getScreenWidth(this) * middleRatio.x, getScreenHeight(this) * middleRatio.y);

            values[2] += middleP.x - (photoView.getDisplayRect().left + (photoView.getDisplayRect().right - photoView.getDisplayRect().left) * curRatio.x);
            values[5] += middleP.y - (photoView.getDisplayRect().top + (photoView.getDisplayRect().bottom - photoView.getDisplayRect().top) * curRatio.y);

            suppMatrix.setValues(values);
            photoView.setSuppMatrix(suppMatrix);
        });
    }

    public static int getScreenHeight(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            return windowMetrics.getBounds().height() - insets.left - insets.right;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        }
    }

    public static int getScreenWidth(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            return windowMetrics.getBounds().width() - insets.left - insets.right;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }


    //라디오 버튼에 대한 검색 타입 설정
    private void createSearchView() {
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        ImageButton searchButton = findViewById(R.id.searchButton);

        radioGroup.check(R.id.rb_stname);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_stname) {
                Toast.makeText(MainActivity.this, "상호명으로 검색", Toast.LENGTH_SHORT).show();
                searchField.setHint("상호명을 입력하세요.");
                adapter.setSearchState("st_name");
                adapter.getFilter().filter(searchField.getText());
            } else if (checkedId == R.id.rb_address) {
                Toast.makeText(MainActivity.this, "주소지로 검색", Toast.LENGTH_SHORT).show();
                searchField.setHint("주소지를 입력하세요.");
                adapter.setSearchState("address");
                adapter.getFilter().filter(searchField.getText());
            } else if (checkedId == R.id.rb_id) {
                Toast.makeText(MainActivity.this, "건물번호로 검색", Toast.LENGTH_SHORT).show();
                searchField.setHint("건물번호를 입력하세요. (예시: 1-3-2)");
                adapter.setSearchState("id");
                adapter.getFilter().filter(searchField.getText());
            }
        });

        searchButton.setOnClickListener(view -> {
            if (searchField.hasFocus()) {
                searchField.clearFocus();
            }
        });

        //입력중일때 추천검색어 고민중
        searchField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ImageView icon = findViewById(R.id.pin);
                icon.setVisibility(View.INVISIBLE);
                adapter.clearRecyclerView();
                searchField.setText("");
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            } else {
                InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
            }
        });

        searchField.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchField.clearFocus();
                return true;
            }
            return false;
        });

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    //버튼 초기화
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void createMapView() {
        photoView = findViewById(R.id.photo_view);
        //줌 비율 설정
        photoView.setMaximumScale(7.0f);
        photoView.setMediumScale(3.8f);
        photoView.setImageResource(R.drawable.operation_map);

        photoView.setScale(2.7f, 900.0f, 0.0f, false);

        Toast.makeText(getApplicationContext(), "로딩 완료", Toast.LENGTH_SHORT).show();
        photoView.setScale(2.7f, 940.0f, 0.0f, false);


        ImageButton[] arcadeButton = new ImageButton[arcadeList.size()];
        FrameLayout mapView = findViewById(R.id.mapView);
        final int[] arcadeImgList = new int[]{R.drawable.arcadepin1, R.drawable.arcadepin2, R.drawable.arcadepin3, R.drawable.arcadepin4,
                R.drawable.arcadepin5, R.drawable.arcadepin6, R.drawable.arcadepin7, R.drawable.arcadepin8};

        ImageButton[] approachButton = new ImageButton[approachList.size()];
        final int[] approachImgList = new int[]{R.drawable.approach_pin_a, R.drawable.approach_pin_b, R.drawable.approach_pin_c, R.drawable.approach_pin_d,
                R.drawable.approach_pin_e, R.drawable.approach_pin_f, R.drawable.approach_pin_g, R.drawable.approach_pin_h,
                R.drawable.approach_pin_i, R.drawable.approach_pin_j, R.drawable.approach_pin_k};

        ImageView[] auto_arcadeIv = new ImageView[auto_arcadeList.size()];  //지상 아케이트 이미지 추가
        final int[] auto_arcadeImgList = new int[]{R.drawable.arcade_a, R.drawable.arcade_b, R.drawable.arcade_c, R.drawable.arcade_d,R.drawable.arcade_e, R.drawable.arcade_f,
                R.drawable.arcade_g, R.drawable.arcade_h, R.drawable.arcade_i, R.drawable.arcade_j, R.drawable.arcade_k, R.drawable.arcade_l, R.drawable.arcade_m,
                R.drawable.arcade_n, R.drawable.arcade_o, R.drawable.arcade_p, R.drawable.arcade_q, R.drawable.arcade_r};

        ImageView[] fireplugIv = new ImageView[fireplugList.size()];       //소화전 이미지 추가
        final int[] fireplugImgList = new int[]{R.drawable.fireplugimg_1, R.drawable.fireplugimg_2, R.drawable.fireplugimg_3, R.drawable.fireplugimg_4,
                R.drawable.fireplugimg_5, R.drawable.fireplugimg_6, R.drawable.fireplugimg_7, R.drawable.fireplugimg_8,
                R.drawable.fireplugimg_9, R.drawable.fireplugimg_10, R.drawable.fireplugimg_11, R.drawable.fireplugimg_12,
                R.drawable.fireplugimg_13, R.drawable.fireplugimg_14, R.drawable.fireplugimg_15, R.drawable.fireplugimg_16,
                R.drawable.fireplugimg_17, R.drawable.fireplugimg_18, R.drawable.fireplugimg_19, R.drawable.fireplugimg_20,
                R.drawable.fireplugimg_21, R.drawable.fireplugimg_22, R.drawable.fireplugimg_23, R.drawable.fireplugimg_24,
                R.drawable.fireplugimg_25, R.drawable.fireplugimg_26, R.drawable.fireplugimg_27};

        float arcadeInitDp = 5f;
        RelativeLayout.LayoutParams arcadeParam = new RelativeLayout.LayoutParams(dpToPx(MainActivity.this, arcadeInitDp * photoView.getScale()), dpToPx(MainActivity.this, arcadeInitDp * photoView.getScale()));
        arcadeParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        float approachInitDp = 15f;
        RelativeLayout.LayoutParams approachParam = new RelativeLayout.LayoutParams(dpToPx(MainActivity.this, approachInitDp * photoView.getScale()), dpToPx(MainActivity.this, approachInitDp * photoView.getScale()));
        approachParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        float auto_arcadeInitDp = 5f;
        RelativeLayout.LayoutParams auto_arcadeParam = new RelativeLayout.LayoutParams(dpToPx(MainActivity.this, auto_arcadeInitDp * photoView.getScale()), dpToPx(MainActivity.this, auto_arcadeInitDp * photoView.getScale()));
        approachParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        float fireplugInitDp = 5f;
        RelativeLayout.LayoutParams fireplugParam = new RelativeLayout.LayoutParams(dpToPx(MainActivity.this, fireplugInitDp * photoView.getScale()), dpToPx(MainActivity.this, fireplugInitDp * photoView.getScale()));
        fireplugParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        for (int i = 0; i < arcadeList.size(); i++) {
            arcadeButton[i] = new ImageButton(this);
            arcadeButton[i].setBackgroundResource(arcadeImgList[arcadeList.get(i).getNum() - 1]);

            arcadeButton[i].setLayoutParams(arcadeParam);
            RectF rect = photoView.getDisplayRect();
            arcadeButton[i].setX((rect.left + ((rect.right - rect.left) * arcadeList.get(i).getX()) - arcadeParam.width / 2.0f));
            arcadeButton[i].setY((rect.top + ((rect.bottom - rect.top) * arcadeList.get(i).getY()) - arcadeParam.height / 2.0f));
            mapView.addView(arcadeButton[i]);
            arcadeButton[i].setVisibility(View.INVISIBLE);
        }

        for (int i = 0; i < approachList.size(); i++) {
            approachButton[i] = new ImageButton(this);
            approachButton[i].setBackgroundResource(approachImgList[approachList.get(i).getNum() - 1]);

            approachButton[i].setLayoutParams(approachParam);
            RectF rect = photoView.getDisplayRect();
            approachButton[i].setX((rect.left + ((rect.right - rect.left) * approachList.get(i).getX()) - approachParam.width/ 2.0f));
            approachButton[i].setY((rect.top + ((rect.bottom - rect.top) * approachList.get(i).getY()) - approachParam.height / 2.0f));
            mapView.addView(approachButton[i]);
        }

        for (int i = 0; i < auto_arcadeList.size(); i++) {
            auto_arcadeIv[i] = new ImageView(this);
            auto_arcadeIv[i].setBackgroundResource(auto_arcadeImgList[auto_arcadeList.get(i).getNum() - 1]);

            auto_arcadeIv[i].setLayoutParams(auto_arcadeParam);
            RectF rect = photoView.getDisplayRect();
            auto_arcadeIv[i].setX((rect.left + ((rect.right - rect.left) * auto_arcadeList.get(i).getX()) - auto_arcadeParam.width/ 2.0f));
            auto_arcadeIv[i].setY((rect.top + ((rect.bottom - rect.top) * auto_arcadeList.get(i).getY()) - auto_arcadeParam.height / 2.0f));
            mapView.addView(auto_arcadeIv[i]);
            auto_arcadeIv[i].setVisibility(View.INVISIBLE);
        }

        for (int i = 0; i < fireplugList.size(); i++) {
            fireplugIv[i] = new ImageView(this);
            fireplugIv[i].setBackgroundResource(fireplugImgList[fireplugList.get(i).getNum() - 1]);

            fireplugIv[i].setLayoutParams(auto_arcadeParam);
            RectF rect = photoView.getDisplayRect();
            fireplugIv[i].setX((rect.left + ((rect.right - rect.left) * fireplugList.get(i).getX()) - fireplugParam.width/ 2.0f));
            fireplugIv[i].setY((rect.top + ((rect.bottom - rect.top) * fireplugList.get(i).getY()) - fireplugParam.height / 2.0f));
            mapView.addView(fireplugIv[i]);
            fireplugIv[i].setVisibility(View.INVISIBLE);
        }

        for (int i = 0; i < arcadeList.size(); i++) {
            int finalI = i;
            arcadeButton[i].setOnClickListener(v -> {
                //검색 느림 추후 개선
                String arcadeId = arcadeList.get(finalI).getId().trim();
                String address = adapter.getItem(arcadeId).getAddress();

                Intent intent = new Intent(getApplicationContext(), ArcadePop.class);
                intent.putExtra("Enter_num", Integer.toString(arcadeList.get(finalI).getNum()));
                intent.putExtra("Address", address);
                intent.putExtra("Detail_info", arcadeList.get(finalI).getDetail());
                startActivityForResult(intent, 1);
            });
        }

        for (int i = 0; i < approachList.size(); i++) {
            int finalI = i;
            approachButton[i].setOnClickListener(v -> {
                Intent intent2 = new Intent(getApplicationContext(), ApproachPop.class);
                intent2.putExtra("Name", approachList.get(finalI).getName());
                intent2.putExtra("Address", approachList.get(finalI).getAddress());
                intent2.putExtra("Num", Integer.toString(approachList.get(finalI).getNum()));
                startActivityForResult(intent2, 1);
            });
        }

        //버튼 사이즈에 대한 실시간 변경
        photoView.setOnMatrixChangeListener(rect -> {
            ImageView icon = findViewById(R.id.pin);
            float pinWidth = icon.getWidth();
            float pinHeight = icon.getHeight();

            icon.setX((rect.left + ((rect.right - rect.left) * curRatio.x) - pinWidth / 2));
            icon.setY((rect.top + ((rect.bottom - rect.top) * curRatio.y) - pinHeight));

            for (int i = 0; i < arcadeList.size(); i++) {
                FrameLayout.LayoutParams tmp = (FrameLayout.LayoutParams) arcadeButton[i].getLayoutParams();
                tmp.width = dpToPx(MainActivity.this,arcadeInitDp * photoView.getScale());
                tmp.height = dpToPx(MainActivity.this,arcadeInitDp * photoView.getScale());
                arcadeButton[i].setLayoutParams(tmp);
            }
            for (int i = 0; i < approachList.size(); i++) {
                FrameLayout.LayoutParams tmp = (FrameLayout.LayoutParams) approachButton[i].getLayoutParams();
                tmp.width = dpToPx(MainActivity.this,approachInitDp * photoView.getScale());
                tmp.height = dpToPx(MainActivity.this,approachInitDp * photoView.getScale());
                approachButton[i].setLayoutParams(tmp);
            }
            for (int i = 0; i < auto_arcadeList.size(); i++) {          //지상 아케이트 크기
                FrameLayout.LayoutParams tmp = (FrameLayout.LayoutParams) auto_arcadeIv[i].getLayoutParams();
                tmp.width = dpToPx(MainActivity.this,auto_arcadeInitDp * photoView.getScale());
                tmp.height = dpToPx(MainActivity.this,auto_arcadeInitDp * photoView.getScale());
                auto_arcadeIv[i].setLayoutParams(tmp);
            }
            for (int i = 0; i < fireplugList.size(); i++) {         //소화전 크기
                FrameLayout.LayoutParams tmp = (FrameLayout.LayoutParams) fireplugIv[i].getLayoutParams();
                tmp.width = dpToPx(MainActivity.this,fireplugInitDp * photoView.getScale());
                tmp.height = dpToPx(MainActivity.this,fireplugInitDp * photoView.getScale());
                fireplugIv[i].setLayoutParams(tmp);
            }

            for (int i = 0; i < arcadeList.size(); i++) {
                arcadeButton[i].setX((rect.left + ((rect.right - rect.left) * arcadeList.get(i).getX()) - arcadeButton[i].getLayoutParams().width / 2.0f));
                arcadeButton[i].setY((rect.top + ((rect.bottom - rect.top) * arcadeList.get(i).getY()) - arcadeButton[i].getLayoutParams().height / 2.0f));
            }
            for (int i = 0; i < approachList.size(); i++) {
                approachButton[i].setX((rect.left + ((rect.right - rect.left) * approachList.get(i).getX()) - approachButton[i].getLayoutParams().width / 2.0f));
                approachButton[i].setY((rect.top + ((rect.bottom - rect.top) * approachList.get(i).getY()) - approachButton[i].getLayoutParams().height / 2.0f));
            }
            for (int i = 0; i < auto_arcadeList.size(); i++) {          //지상 아케이트 크기
                auto_arcadeIv[i].setX((rect.left + ((rect.right - rect.left) * auto_arcadeList.get(i).getX()) - auto_arcadeIv[i].getLayoutParams().width / 2.0f));
                auto_arcadeIv[i].setY((rect.top + ((rect.bottom - rect.top) * auto_arcadeList.get(i).getY()) - auto_arcadeIv[i].getLayoutParams().height / 2.0f));
            }
            for (int i = 0; i < fireplugList.size(); i++) {         //소화전 크기
                fireplugIv[i].setX((rect.left + ((rect.right - rect.left) * fireplugList.get(i).getX()) - fireplugIv[i].getLayoutParams().width / 2.0f));
                fireplugIv[i].setY((rect.top + ((rect.bottom - rect.top) * fireplugList.get(i).getY()) - fireplugIv[i].getLayoutParams().height / 2.0f));
            }
        });

        Switch fireplugSwitch = findViewById(R.id.fireplug_switch);
        Switch arcadeSwitch = findViewById(R.id.arcade_switch);

        //소화전 버튼의 가시성 여부
        fireplugSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (int i = 0; i < fireplugList.size(); i++) {
                    if (fireplugIv[i].getVisibility() == View.VISIBLE)
                        fireplugIv[i].setVisibility(View.INVISIBLE);
                    else fireplugIv[i].setVisibility(View.VISIBLE);
                }
            }
        });

        //아케이드 버튼의 가시성 여부
        arcadeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (int i = 0; i < arcadeList.size(); i++) {
                    if (arcadeButton[i].getVisibility() == View.VISIBLE)
                        arcadeButton[i].setVisibility(View.INVISIBLE);
                    else arcadeButton[i].setVisibility(View.VISIBLE);
                }
                for (int i = 0; i < auto_arcadeList.size(); i++) {
                    if (auto_arcadeIv[i].getVisibility() == View.VISIBLE)
                        auto_arcadeIv[i].setVisibility(View.INVISIBLE);
                    else auto_arcadeIv[i].setVisibility(View.VISIBLE);
                }
            }
        });

        Button optionButton = findViewById(R.id.optionButton);
        DrawerLayout drawer = findViewById(R.id.drawer);
        optionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                drawer.openDrawer(GravityCompat.END);
            }
        });
    }

    public int dpToPx(Context context, float dp) {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
        return px;
    }
}