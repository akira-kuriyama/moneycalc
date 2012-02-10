package com.marronst.moneycalc.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.marronst.moneycalc.dxo.CategoryDxo;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.Category.CnCategory;
import com.marronst.moneycalc.entity.Category.TnCategory;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class CategoryDao {

	SQLiteDatabase db;

	@SuppressWarnings("unused")
	private CategoryDao() {
		//
	}

	public CategoryDao(final SQLiteDatabase db) {
		this.db = db;
	}

	//  create table  ----------------------------------------

	/** テーブル作成  */
	public void createTable() {
		StringBuffer sb = new StringBuffer();
		sb.append(" create table " + TnCategory.tableName() + " (");
		sb.append(CnCategory.id() + " integer primary key autoincrement, ");
		sb.append(CnCategory.categoryName() + " text not null, ");
		sb.append(CnCategory.position() + " integer not null, ");
		sb.append(CnCategory.registerDate() + " text default current_timestamp, ");
		sb.append(CnCategory.dispFlg() + " integer not null default " + Category.DISP_FLG_ON + ",");
		sb.append(CnCategory.incomeFlg() + " integer not null, ");
		sb.append(CnCategory.iconName() + " text ");
		sb.append(");");

		db.execSQL(sb.toString());
	}

	//  挿入  ----------------------------------------

	/** カテゴリレコードを挿入する */
	public long insert(final String categoryName, final int incomeFlg) {
		ContentValues values = new ContentValues();
		values = new ContentValues();
		int position = getCategoryMaxIndex();
		values.put(CnCategory.position(), position);
		values.put(CnCategory.categoryName(), categoryName);
		values.put(CnCategory.incomeFlg(), incomeFlg);
		long id = db.insert(TnCategory.tableName(), null, values);
		return id;
	}

	/** テーブルにデフォルトデータを挿入する */
	public void initCategoryTable() {
		int categoryIndex = 0;
		if (KakeiboUtils.isJapan()) {
			insertCategory(categoryIndex++, "食費", Category.INCOME_FLG_OFF);//1
			insertCategory(categoryIndex++, "本", Category.INCOME_FLG_OFF);//2
			insertCategory(categoryIndex++, "交通費", Category.INCOME_FLG_OFF);//3
			insertCategory(categoryIndex++, "交際費", Category.INCOME_FLG_OFF);//4
			insertCategory(categoryIndex++, "衣服費", Category.INCOME_FLG_OFF);//5
			insertCategory(categoryIndex++, "公共料金", Category.INCOME_FLG_OFF);//6
			insertCategory(categoryIndex++, "臨時収入", Category.INCOME_FLG_ON);//7
			insertCategory(categoryIndex++, "その他", Category.INCOME_FLG_OFF);//8
		} else {
			insertCategory(categoryIndex++, "Food", Category.INCOME_FLG_OFF);//1
			insertCategory(categoryIndex++, "Book", Category.INCOME_FLG_OFF);//2
			insertCategory(categoryIndex++, "Music", Category.INCOME_FLG_OFF);//3
			insertCategory(categoryIndex++, "Bonus", Category.INCOME_FLG_ON);//4
			insertCategory(categoryIndex++, "Others", Category.INCOME_FLG_OFF);//5

		}

	}

	public void createDummyData() {

		//		insertCategory(categoryIndex++, "食事", Category.INCOME_FLG_OFF);//1
		//		insertCategory(categoryIndex++, "漫画・雑誌・小～説・単行本", Category.INCOME_FLG_OFF);//2
		//		insertCategory(categoryIndex++, "参考書", Category.INCOME_FLG_OFF);//3
		//		insertCategory(categoryIndex++, "公共料金", Category.INCOME_FLG_OFF);//4
		//		insertCategory(categoryIndex++, "服", Category.INCOME_FLG_OFF);//5
		//		insertCategory(categoryIndex++, "収入", Category.INCOME_FLG_ON);//6
		//		insertCategory(categoryIndex++, "交通費！", Category.INCOME_FLG_OFF);//7
		//		insertCategory(categoryIndex++, "お菓子", Category.INCOME_FLG_OFF);//8
		insert("スーパー缶コーヒー", Category.INCOME_FLG_OFF);//9
		insert("雑貨品", Category.INCOME_FLG_OFF);//10
		insert("その他2", Category.INCOME_FLG_OFF);//11
		insert("お弁当なの", Category.INCOME_FLG_ON);//12	

	}

	/** カテゴリレコdードを挿入する(簡易版) */
	private void insertCategory(final int categoryIndex, final String categoryName, final Integer incomeFlg) {
		ContentValues values;
		values = new ContentValues();
		values.put(CnCategory.position(), categoryIndex);
		values.put(CnCategory.categoryName(), categoryName);
		values.put(CnCategory.incomeFlg(), incomeFlg);
		db.insert(TnCategory.tableName(), null, values);
	}

	//  更新  ----------------------------------------

	/** カテゴリレコードを更新する */
	public void update(final Category targetCategory, final int movePosition) {

		List<Category> categoryList = new ArrayList<Category>();
		Cursor c = findAllWithNonDisplay();
		CategoryDxo categoryDxo = new CategoryDxo();
		while (c.moveToNext()) {
			categoryList.add(categoryDxo.createFromCursol(c));
		}
		c.close();

		int updatePosition = 0;
		for (Category category : categoryList) {
			if (category.id.equals(targetCategory.id)) {
				category.categoryName = targetCategory.categoryName;
				category.dispFlg = targetCategory.dispFlg;
				category.incomeFlg = targetCategory.incomeFlg;
				updatePosition = category.position;
			}
		}
		//Log.i("categoryDao 1 updatePosition=", "" + updatePosition);
		Category category = categoryList.remove(updatePosition);
		updatePosition += movePosition;
		//Log.i("categoryDao 2 updatePosition=", "" + updatePosition);
		updatePosition = updatePosition < 0 ? 0 : updatePosition;
		updatePosition = updatePosition > categoryList.size() ? categoryList.size() : updatePosition;

		categoryList.add(updatePosition, category);

		int position = 0;
		for (Category updateCategory : categoryList) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(CnCategory.position(), position);
			if (updateCategory.id.equals(targetCategory.id)) {
				contentValues.put(CnCategory.categoryName(), updateCategory.categoryName);
				contentValues.put(CnCategory.dispFlg(), updateCategory.dispFlg);
				contentValues.put(CnCategory.incomeFlg(), updateCategory.incomeFlg);
			}
			db.update(TnCategory.tableName(),//
						contentValues,//
						CnCategory.id() + " = ?",//
						new String[] { updateCategory.id.toString() });
			position++;
		}
	}

	//  検索  ----------------------------------------

	/** 全件検索(非表示除く) */
	public Cursor findAll() {

		StringBuffer findAllSql = new StringBuffer();
		findAllSql.append(" select * from " + TnCategory.tableName());
		findAllSql.append(" where " + CnCategory.dispFlg() + " =  " + Category.DISP_FLG_ON);
		findAllSql.append(" order by   " + CnCategory.position() + " asc");
		findAllSql.append(";");
		Cursor c = db.rawQuery(findAllSql.toString(), null);
		return c;
	}

	/**　全件検索(非表示含む)　*/
	public Cursor findAllWithNonDisplay() {

		StringBuffer findAllWithDeleted = new StringBuffer();
		findAllWithDeleted.append(" select * from " + TnCategory.tableName());
		findAllWithDeleted.append(" order by   " + CnCategory.position() + " asc");
		findAllWithDeleted.append(";");
		Cursor c = db.rawQuery(findAllWithDeleted.toString(), null);
		return c;
	}

	/**　カテゴリ名をもとに検索*/
	public Category findIdByCategoryName(final String categoryname) {

		Cursor c = db.query(TnCategory.tableName(), null, CnCategory.categoryName() + " = ?",
							new String[] { categoryname }, null, null, null);

		Category category = null;
		while (c.moveToNext()) {
			category = new CategoryDxo().createFromCursol(c);
		}
		c.close();
		return category;
	}

	/**　IDをもとに検索　*/
	public Category findById(final String id) {

		Cursor c = db.query(TnCategory.tableName(), null, CnCategory.id() + " = ?", new String[] { id },
							null, null, null);

		Category category = null;
		while (c.moveToNext()) {
			category = new CategoryDxo().createFromCursol(c);
		}
		c.close();
		return category;
	}

	//  削除  ----------------------------------------

	/**　IDをもとに削除　*/
	public void deleteById(final Integer id) {

		StringBuffer deleteByIdSql = new StringBuffer();
		deleteByIdSql.append(" delete from " + TnCategory.tableName());
		deleteByIdSql.append(" where " + CnCategory.id() + " = " + id);
		deleteByIdSql.append(";");

		db.execSQL(deleteByIdSql.toString());

		HouseKeepingBookDao houseKeepingBookDao = new HouseKeepingBookDao(db);
		houseKeepingBookDao.deleteByCategoryId(id);

		//並び順を更新
		List<Category> categoryList = new ArrayList<Category>();
		Cursor c = findAllWithNonDisplay();
		CategoryDxo categoryDxo = new CategoryDxo();
		while (c.moveToNext()) {
			categoryList.add(categoryDxo.createFromCursol(c));
		}
		c.close();

		int position = 0;
		for (Category updateCategory : categoryList) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(CnCategory.position(), position);
			db.update(TnCategory.tableName(),//
						contentValues,//
						CnCategory.id() + " = ?",//
						new String[] { updateCategory.id.toString() });
			position++;
		}
	}

	/**
	 * 全削除
	 */
	public void deleteAll() {
		db.delete(TnCategory.tableName(), null, null);
	}

	//　内部メソッド ----------------------------------------

	/** カテゴリ順序の最大値を取得 */
	private int getCategoryMaxIndex() {

		StringBuffer getCategoryMaxIndexSql = new StringBuffer();
		getCategoryMaxIndexSql.append(" select max(" + CnCategory.position() + "), count(" + CnCategory.id()
				+ ") from " + TnCategory.tableName());
		getCategoryMaxIndexSql.append(";");

		Cursor c = db.rawQuery(getCategoryMaxIndexSql.toString(), null);
		c.moveToNext();
		int index = c.getInt(0);
		int count = c.getInt(1);

		if (count > 0) {
			index++;
		}
		c.close();

		return index;
	}

}
