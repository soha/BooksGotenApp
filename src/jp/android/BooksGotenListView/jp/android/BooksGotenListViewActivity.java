package jp.android.BooksGotenListView.jp.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jp.android.BooksGotenListView.R;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 書籍の一覧表示
 * @author you
 *
 */
public class BooksGotenListViewActivity extends Activity {
	
	static final int ITEMS_PER_PAGE = 3;
	ListView booksListView;
	int offset = 0;
	BooksAdaptor adaptor;
	BooksAdaptor viewAdaptor;
	HashMap<String,Drawable> cache = new HashMap<String,Drawable>();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		booksListView = (ListView) findViewById(R.id.BooksListView);
		
		booksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

		    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		        ListView listView = (ListView) parent;
		        // クリックされたアイテムを取得します
		        //Toast.makeText(BooksGotenListViewActivity.this, position + "クリック", 0).show();
		        Book book = (Book) listView.getItemAtPosition(position);
		        
		        
		        if(book.isNextButton){
		        	offset++;
		    		BooksRequestTask task = new BooksRequestTask(BooksGotenListViewActivity.this, booksListView);
		    		task.execute(String.valueOf(offset)); 
		        }else{
			        Intent intent = new Intent(getApplicationContext(), BookDetailActivity.class);
			        intent.putExtra("book", book);
			        startActivity(intent);
		        }
		    }

		});
		
		BooksRequestTask task = new BooksRequestTask(this, booksListView);
		task.execute(); 
		
	}


	/**
	 * オプションメニュー作成
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST, Menu.NONE, "reload");

		return ret;
	}
	
	/**
	 * オプションメニュー押下時
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean res = super.onOptionsItemSelected(item);

		BooksRequestTask task = new BooksRequestTask(this, this.booksListView);
		task.execute(); 

		return res;
	}
	
	/**
	 * 画面復帰時に呼び出される
	 */
	@Override
	public void onResume() {
		super.onResume();
		BooksRequestTask task = new BooksRequestTask(this, this.booksListView);
		task.execute(); 
	}
	
	public class BooksRequestTask extends AsyncTask<String, Integer, BooksData> {

		ListView booksListView;
		Activity activity;
		ProgressDialog progressDialog;
		
		public BooksRequestTask(Activity activity, ListView booksListView) {
			this.activity = activity;
			this.booksListView = booksListView;
			viewAdaptor = new BooksAdaptor(activity, R.layout.book_row, new ArrayList<Book>());
		}
		
		@Override
		protected void onPreExecute() {
			// プログレスバー設定  
	        progressDialog = new ProgressDialog(activity);  
	        progressDialog.setTitle("書籍データのダウンロード中");  
	        progressDialog.setIndeterminate(false);  
	        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);  
            //progressDialog.setMax(100); // 進捗最大値を設定  

	        progressDialog.show(); 			
	        //Toast.makeText(context, "書籍データを取得しています。", 0).show();
		}

		@Override
		protected BooksData doInBackground(String... params) {
			offset = 0;
			try{
				if(params.length > 0){
					offset = Integer.parseInt(params[0]);
				}
			}catch(NumberFormatException e){}
			return getBooksData(offset);
		}
		
		@Override
		protected void onPostExecute(BooksData booksData) {
			progressDialog.dismiss(); //プログレスバー消す
			Toast.makeText(activity, "書籍データ取得完了", Toast.LENGTH_SHORT).show();
			
			
			if(offset > 0 && adaptor != null){
				//既にアダプター設定済み、後ろに追加する
				adaptor.addItems(booksData.books);
			}else{
				// アダプターを設定します
				adaptor = new BooksAdaptor(activity, R.layout.book_row, booksData.books);
			}
			viewAdaptor.clear();
			viewAdaptor.addItems(adaptor.items);
			
			int offset_count = booksData.offset_count;
			int total_count = booksData.total_count;
			if(offset_count + ITEMS_PER_PAGE < total_count) {
				//未取得の次のアイテムがある
				Book nextBooks = new Book();
				nextBooks.title = "次の本達";
				nextBooks.author = "";
				nextBooks.publisher = "";
				nextBooks.isNextButton = true;
				viewAdaptor.add(nextBooks);
			}
			booksListView.setAdapter(viewAdaptor);
			//offset分スクロール
			booksListView.setSelectionFromTop(offset_count, 0);
		}
		
		/**
		 * サーバから書籍データを取得する
		 * @param offset 取得ページのoffset
		 * @return
		 */
		private BooksData getBooksData(int offset) {
			
			BooksData booksData = new BooksData();
			
			try {
				URL booksUrl = new URL("http://booksgoten.appspot.com/catalog/list/" + offset);
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(booksUrl.openStream());
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				
				String key = "";
				String title = "";
				String image_url = "";
				String author = "";
				String publisher = "";
				Date publication_date = null;
				int price = 0;
				boolean lending = false;
				String detail_page_shop_url = "";
				
				Node tNode = doc.getElementById("offset_count");
				if(tNode != null) {
					booksData.offset_count = Integer.parseInt(tNode.getFirstChild().getNodeValue());
				}
				tNode = doc.getElementById("total_count");
				if(tNode != null) {
					booksData.total_count = Integer.parseInt(tNode.getFirstChild().getNodeValue());
				}

				NodeList bookList = doc.getElementsByTagName("book");
				for(int i=0; i<bookList.getLength(); i++) {
					Node bookNode = bookList.item(i);
					String nodeName = bookNode.getNodeName();
						NodeList bookProps = bookNode.getChildNodes();
						for(int j=0; j<bookProps.getLength(); j++) {
							Node prop = bookProps.item(j);
							nodeName = prop.getNodeName();
							if("title".equals(nodeName)){
								title = prop.getFirstChild().getNodeValue();
							}else if("image_url".equals(nodeName)) {
								image_url = prop.getFirstChild().getNodeValue();
							}else if("author".equals(nodeName)) {
								author = prop.getFirstChild().getNodeValue();
							}else if("publisher".equals(nodeName)) {
								publisher = prop.getFirstChild().getNodeValue();
							}else if("publication_date".equals(nodeName)) {
								String publication_date_str = prop.getFirstChild().getNodeValue();
								try {
									publication_date = sdf.parse(publication_date_str);
								} catch (ParseException e) {
									// 未入力や日付の書式が異なる場合はなしとする
									publication_date = null;
								}
							}else if("price".equals(nodeName)) {
								String price_str = prop.getFirstChild().getNodeValue();
								price = Integer.parseInt(price_str);
							}else if("lending".equals(nodeName)) {
								String lending_str = prop.getFirstChild().getNodeValue();
								lending = Boolean.parseBoolean(lending_str);
							}else if("key".equals(nodeName)) {
								key = prop.getFirstChild().getNodeValue();;
							}else if("detail_page_shop_url".equals(nodeName)) {
								detail_page_shop_url = prop.getFirstChild().getNodeValue();;
							}
						}
						Book b = new Book();
						b.key = key;
						b.title = title;
						b.image_url = image_url;
						b.author = author;
						b.publisher = publisher;
						b.publication_date = publication_date;
						b.price = price;
						b.lending = lending;
						b.detail_page_shop_url = detail_page_shop_url;
						booksData.books.add(b);
				}
			} catch (MalformedURLException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			return booksData;
		}
	}
	
	
	
	
	/**
	 * 書籍データの一覧ビュー
	 * @author you
	 *
	 */
	class BooksAdaptor extends ArrayAdapter<Book> {
		private List<Book> items;
		private LayoutInflater inflater;
		Context context;

		public BooksAdaptor(Context context, int resource, List<Book> items) {
			super(context, resource, items);
			this.context = context;
			this.items = items;
			this.inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public void addItems(List<Book> books){
			this.items.addAll(books);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View v = convertView;
			if(v == null){  
	            //1行分layoutからViewの塊を生成
	            v = inflater.inflate(R.layout.book_row, null);  
	        }
			
			Book book = items.get(position);
			
			TextView bookTitle = (TextView)v.findViewById(R.id.BookTitleView);
			bookTitle.setText(book.title);

			ImageView bookImage = (ImageView)v.findViewById(R.id.BookImageView);

			if(book.isNextButton) {
	            bookImage.setVisibility(View.GONE);
			}else{
				bookTitle.setText(bookTitle.getText() + "\n\n" + book.author + "\n" + book.publisher);

				Drawable image = ImageOperations(context, book.image_url);
				bookImage.setImageDrawable(image);
				bookImage.setVisibility(View.VISIBLE);
			}

			return v;
		}
		
		
		private Drawable ImageOperations(Context ctx, String url) {
			
			Drawable d = getCacheImage(url);
			if(d != null) return d;
			
			try {
				InputStream is = (InputStream) this.fetch(url);
				d = Drawable.createFromStream(is, "src");
				cacheImage(url, d);
				return d;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		private Object fetch(String address) throws MalformedURLException,IOException {
			URL url = new URL(address);
			Object content = url.getContent();
			return content;
		}
		
		private Drawable getCacheImage(String url) {
			if (cache.containsKey(url)) {  
	            Log.d("cache", "cache hit!");  
	            return cache.get(url);
	            //return null;
	        }  
	        return null;  
		}
		
		private void cacheImage(String url, Drawable image) {
			cache.put(url, image);
		}
	}


}