package com.marronst.moneycalc.utils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import android.graphics.Bitmap;

/**
 * グラフキャッシュUtil
 * @author akira
 *
 */
public class ImageCacheUtils {

	private static LinkedHashMap<String, Bitmap> map = new LinkedHashMap<String, Bitmap>();

	public static Bitmap get(final String url) {
		Bitmap bitmap = map.get(url);
		return bitmap;
	}

	public static void put(final String url, final Bitmap image) {

		//			for (Entry<String, Bitmap> entry : map.entrySet()) {
		//				Log.w(TAG, "url1=" + entry.getKey());
		//			}

		if (map.size() >= 6) {
			Iterator<Entry<String, Bitmap>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
				break;
			}
		}

		//			for (Entry<String, Bitmap> entry : map.entrySet()) {
		//				Log.w(TAG, "url2=" + entry.getKey());
		//			}
		map.put(url, image);
	}
}
