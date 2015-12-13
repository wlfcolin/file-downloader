package org.wlf.filedownloader_demo2.advanced_use;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.RadioGroup;

import org.wlf.action_tab_pager.v4.FragmentActionTabPager;
import org.wlf.action_tab_pager.view.ActionTabViewPager;
import org.wlf.filedownloader_demo2.R;
import org.wlf.filedownloader_demo2.advanced_use.course_download.CourseDownloadFragment;
import org.wlf.filedownloader_demo2.advanced_use.course_preview.CoursePreviewFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo2 Test——AdvancedUseActivity
 * <br/>
 * 测试2高级用法界面
 *
 * @author wlf(Andy)
 * @datetime 2015-12-11 21:40 GMT+8
 * @email 411086563@qq.com
 */
public class AdvancedUseActivity extends FragmentActivity {

    private FragmentActionTabPager mFragmentActionTabPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advanced_use__activity_advanced_use);

        // init fragments
        List<Fragment> pagerFragments = new ArrayList<Fragment>();
        Fragment fragment1 = CoursePreviewFragment.newInstance();
        pagerFragments.add(fragment1);
        Fragment fragment2 = CourseDownloadFragment.newInstance();
        pagerFragments.add(fragment2);

        mFragmentActionTabPager = new FragmentActionTabPager(getSupportFragmentManager(), pagerFragments);

        RadioGroup rgActionTab = (RadioGroup) findViewById(R.id.rgActionTab);
        ActionTabViewPager avPagerContainer = (ActionTabViewPager) findViewById(R.id.atViewPager);
        mFragmentActionTabPager.setup(rgActionTab, avPagerContainer, true, false);
    }
}
