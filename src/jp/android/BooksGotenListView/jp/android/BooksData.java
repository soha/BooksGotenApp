package jp.android.BooksGotenListView.jp.android;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 書籍データ一覧
 * @author you
 *
 */
public class BooksData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 現在表示済みの件数
	 */
	public int offset_count = 0;
	
	/**
	 * トータル件数
	 * サーバが返した最大件数 GoogleAppEngineの仕様から1000件がMAXと思われる
	 */
	public int total_count = 0;
	
	/**
	 * 書籍データ一覧
	 */
	public List<Book> books = new ArrayList<Book>();
}
