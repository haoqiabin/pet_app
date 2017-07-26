package cn.haoqiabin.pet.base;

import android.app.Application;
import cn.haoqiabin.pet.utils.PreferenceUtil;

public class AppData extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceUtil.init(this);
	}

}
