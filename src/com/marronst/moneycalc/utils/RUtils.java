package com.marronst.moneycalc.utils;

public class RUtils {
	//private static final HashMap<String, Integer> drawableMap = new HashMap<String, Integer>();
	//
	//	//★コンストラクタ
	//	static {
	//		//Rクラスの全ての内部クラスを取得
	//		Class<?>[] classes = com.marronst.moneycalc.R.class.getClasses();
	//
	//		for (Class<?> cls : classes) {
	//			//例：内部クラスがdrawableの場合のみコレクション作成
	//			if (cls.getSimpleName().equalsIgnoreCase("drawable")) {
	//				Field[] fields = cls.getFields();
	//				String name;
	//				for (Field field : fields) {
	//					try {
	//						name = field.getName();
	//						//iconは取得しない
	//						if (name.equals("icon")) {
	//							continue;
	//						}
	//						//コレクションに格納
	//						drawableMap.put(name, ((Integer) field.get(name)));
	//					} catch (IllegalArgumentException e) {
	//						e.printStackTrace();
	//					} catch (IllegalAccessException e) {
	//						e.printStackTrace();
	//					}
	//				}
	//			}
	//		}
	//	}
	//
	//	public static int getDrawableResId(final String imageName) {
	//		Integer resId = drawableMap.get(imageName);
	//		if (resId == null) {
	//			Log.i("TAG", "resId == null");
	//
	//		}
	//		return resId;
	//	}
}
