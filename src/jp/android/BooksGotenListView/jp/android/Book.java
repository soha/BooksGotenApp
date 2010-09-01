package jp.android.BooksGotenListView.jp.android;

import java.io.Serializable;
import java.util.Date;

public class Book implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * サーバ側でのキー
	 * GoogleAppEngineのデータストア上のキー
	 */
	public String key;
	/**
	 * Amazon商品番号(Amazon Standard Identification Number)
	 */
	public String asin;
	public String isbn;
	public String title;
	public String author;
	public String publisher;
	public Date publication_date;
	public int price;
	public String image_url;
	
	public String memo;
	/**
	 * 削除予定フラグ(true:削除予定)
	 */
	public boolean deletion_reserve;
	/**
	 * 貸し出し状態(true:貸出中)
	 */
	public boolean lending;
	public Date updated;
	public Date created;
}
