package ml.puredark.hviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.gc.materialdesign.views.ButtonFlat;
import com.github.clans.fab.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ml.puredark.hviewer.HViewerApplication;
import ml.puredark.hviewer.R;
import ml.puredark.hviewer.beans.Collection;
import ml.puredark.hviewer.beans.Site;
import ml.puredark.hviewer.beans.SiteGroup;
import ml.puredark.hviewer.helpers.HViewerHttpClient;
import ml.puredark.hviewer.helpers.MDStatusBarCompat;
import ml.puredark.hviewer.helpers.SitePropViewHolder;
import ml.puredark.hviewer.holders.SiteHolder;


public class AddSiteActivity extends AnimationActivity {

    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.edittext_container)
    RelativeLayout edittextContainer;
    @BindView(R.id.view_add_site_json)
    View viewAddSiteJson;
    @BindView(R.id.view_site_details)
    View viewSiteDetails;
    @BindView(R.id.btn_return)
    ImageView btnReturn;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.input_site)
    MaterialEditText inputSite;
    @BindView(R.id.btn_parse_json)
    ButtonFlat btnParseJson;

    @BindView(R.id.fab_submit)
    FloatingActionButton fabSubmit;

    private Pair<SiteGroup, List<Site>> pair;

    private SitePropViewHolder holder;

    private SiteHolder siteHolder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_site);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        MDStatusBarCompat.setOrdinaryToolBar(this);

        setContainer(coordinatorLayout);

        /* 为返回按钮加载图标 */
        setReturnButton(btnReturn);

        //获取传递过来的SiteGroup实例
        if (HViewerApplication.temp instanceof Pair)
            pair = (Pair<SiteGroup, List<Site>>) HViewerApplication.temp;

        //获取失败则结束此界面
        if (pair == null) {
            finish();
            return;
        }

        holder = new SitePropViewHolder(viewSiteDetails);
        siteHolder = new SiteHolder(this);

    }

    @OnClick(R.id.btn_return)
    void back() {
        onBackPressed();
    }

    @OnClick(R.id.btn_json_input)
    void switchBetweenJsonAndDetail() {
        if (viewAddSiteJson.getVisibility() == View.GONE) {
            viewAddSiteJson.setVisibility(View.VISIBLE);
            viewSiteDetails.setVisibility(View.GONE);
            fabSubmit.setVisibility(View.GONE);
        } else {
            viewAddSiteJson.setVisibility(View.GONE);
            viewSiteDetails.setVisibility(View.VISIBLE);
            fabSubmit.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.btn_qr_scan)
    void scan() {
        IntentIntegrator integrator = new IntentIntegrator(AddSiteActivity.this);
        integrator.setCaptureActivity(MyCaptureActivity.class);
        integrator.setOrientationLocked(true);
        integrator.setPrompt("请扫描二维码");
        integrator.addExtra("SCAN_WIDTH", 480);
        integrator.addExtra("SCAN_HEIGHT", 480);
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    @OnClick(R.id.btn_parse_json)
    void parseJson() {
        String rule = inputSite.getText().toString();
        Site newSite = parseSite(rule);
        if (newSite == null)
            return;
        holder.fillSitePropEditText(newSite);
        switchBetweenJsonAndDetail();
    }

    @OnClick(R.id.fab_submit)
    void submit() {
        Site newSite = holder.fromEditTextToSite();
        if (newSite == null) {
            showSnackBar("规则缺少必要参数，请检查");
            return;
        }

        newSite.gid = pair.first.gid;
        siteHolder.addSite(newSite);
        int sid = siteHolder.getMaxSiteId();
        newSite.sid = sid;
        newSite.index = sid;
        siteHolder.updateSiteIndex(newSite);

        pair.second.add(newSite);

        HViewerApplication.temp = newSite;
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null && result.getContents() != null) {
            HViewerHttpClient.get(result.getContents(), null, new HViewerHttpClient.OnResponseListener() {
                @Override
                public void onSuccess(String contentType, Object result) {
                    Site newSite = parseSite((String) result);
                    if (newSite == null)
                        return;
                    holder.fillSitePropEditText(newSite);
                }

                @Override
                public void onFailure(HViewerHttpClient.HttpError error) {
                    showSnackBar(error.getErrorString());
                }
            });
        }
    }

    private Site parseSite(String json) {
        try {
            Site site = new Gson().fromJson(json, Site.class);
            if (site == null || site.indexUrl == null || site.galleryUrl == null ||
                    site.indexRule.item == null || site.indexRule.idCode == null ||
                    site.galleryRule.item == null || site.galleryRule.pictureUrl == null)
                showSnackBar("输入的规则缺少信息");
            return site;
        } catch (JsonSyntaxException e) {
            showSnackBar("输入规则格式错误");
            return null;
        }
    }

}
