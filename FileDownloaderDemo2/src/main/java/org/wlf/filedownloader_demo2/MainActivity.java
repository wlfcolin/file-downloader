package org.wlf.filedownloader_demo2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.wlf.filedownloader.FileDownloadManager;
import org.wlf.filedownloader_demo2.custom_model.CustomModelActivity;

/**
 * Demo2 Test MainActivity
 * <br/>
 * 测试2主界面
 *
 * @author wlf(Andy)
 * @datetime 2015-12-05 10:10 GMT+8
 * @email 411086563@qq.com
 */
public class MainActivity extends Activity implements OnItemClickListener {

    private ListView lvList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main__activity_mian);

        lvList = (ListView) findViewById(R.id.lvList);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1);
        
        adapter.add(getString(R.string.custom_model__title_name));
        
        lvList.setAdapter(adapter);
        lvList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == lvList) {
            Intent intent = null;
            switch (position) {
                case 0:
                    intent = new Intent(this, CustomModelActivity.class);
                    break;
            }
            if (intent != null) {
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // pause all downloads
        FileDownloadManager.getInstance(this).pauseAll();
    }
}
