package jp.android.BooksGotenListView.jp.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jp.android.BooksGotenListView.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 書籍の詳細表示
 * 
 * @author you
 * 
 */
public class BookDetailActivity extends Activity implements OnClickListener {

	TextView bookDetailText;
	ImageView bookDetailImage;
	Button lendingButton;
	Book book;
	private HashMap<String, Drawable> cache = new HashMap<String, Drawable>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detail);

		bookDetailText = (TextView) findViewById(R.id.BookDetailView);
		bookDetailImage = (ImageView) findViewById(R.id.BookImageDetailView);
		lendingButton = (Button) findViewById(R.id.LendingButton);
		
		bookDetailImage.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				String url = book.detail_page_shop_url;
				if(!"".equals(url) && !"None".equals(url)) {
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(i);
				}
				
			}
		});

		lendingButton.setOnClickListener(this);

		Intent intent = getIntent();
		book = (Book) intent.getSerializableExtra("book");

		textDraw(book);

		// 画像の取得・表示
		RequestImageTask task = new RequestImageTask(this, bookDetailImage);
		task.execute(book.image_url);

	}

	private void textDraw(Book book) {
		bookDetailText.setText(book.title);
		bookDetailText.setText(bookDetailText.getText() + "\n\n" + book.author
				+ "\n" + book.publisher);
		bookDetailText.setText(bookDetailText.getText() + "\n" + book.price
				+ "円");
		String lendingStr = "";
		if (book.lending) {
			lendingStr = "貸出中";
			lendingButton.setText("返却");

		} else {
			lendingStr = "棚に在り";
			lendingButton.setText("借りる");
		}
		bookDetailText.setText(bookDetailText.getText() + "\n\n" + lendingStr);
	}

	public void onClick(View v) {

		if(v == lendingButton) {
			String lendUrlStr = "http://booksgoten.appspot.com/catalog/lendbydroid/"
					+ book.key;
	
			// 画像の取得・表示
			UpdateLendingStatus task = new UpdateLendingStatus(this);
			task.execute(lendUrlStr);
			
		}
	}

	public class UpdateLendingStatus extends
			AsyncTask<String, Integer, HttpEntity> {

		Activity activity;
		ProgressDialog progressDialog;

		public UpdateLendingStatus(Activity activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			// プログレスバー設定  
	        progressDialog = new ProgressDialog(activity);  
	        progressDialog.setTitle("更新確認中");  
	        progressDialog.setIndeterminate(false);  
	        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);  
            //progressDialog.setMax(100); // 進捗最大値を設定  

	        progressDialog.show(); 			
	        //Toast.makeText(context, "書籍データを取得しています。", 0).show();
		}

		
		@Override
		protected HttpEntity doInBackground(String... params) {
			String url = params[0];
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url);
			boolean lend = !book.lending;
			List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
			nameValuePair.add(new BasicNameValuePair("lending", Boolean
					.toString(lend)));
			try {
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePair));
			} catch (UnsupportedEncodingException e) {
				return null;
			}
			HttpResponse res;
			try {
				res = httpclient.execute(httppost);
			} catch (ClientProtocolException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
			return res.getEntity();
		}

		@Override
		protected void onPostExecute(HttpEntity entity) {
			progressDialog.dismiss(); //プログレスバー消す
			if (entity != null) {
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db;
				try {
					db = dbf.newDocumentBuilder();
				} catch (ParserConfigurationException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
					return;
				}
				Document doc;
				try {
					doc = db.parse(entity.getContent());
				} catch (IllegalStateException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
					return;
				} catch (SAXException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
					return;
				} catch (IOException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
					return;
				}
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

				String key = "";
				String title = "";
				String image_url = "";
				String author = "";
				String publisher = "";
				Date publication_date = null;
				int price = 0;
				boolean lending = false;
				NodeList bookList = doc.getElementsByTagName("book");
				for (int i = 0; i < bookList.getLength(); i++) {
					Node bookNode = bookList.item(i);
					NodeList bookProps = bookNode.getChildNodes();
					for (int j = 0; j < bookProps.getLength(); j++) {
						Node prop = bookProps.item(j);
						String nodeName = prop.getNodeName();
						if ("title".equals(nodeName)) {
							title = prop.getFirstChild().getNodeValue();
						} else if ("image_url".equals(nodeName)) {
							image_url = prop.getFirstChild().getNodeValue();
						} else if ("author".equals(nodeName)) {
							author = prop.getFirstChild().getNodeValue();
						} else if ("publisher".equals(nodeName)) {
							publisher = prop.getFirstChild().getNodeValue();
						} else if ("publication_date".equals(nodeName)) {
							String publication_date_str = prop.getFirstChild()
									.getNodeValue();
							try {
								try {
									publication_date = sdf
											.parse(publication_date_str);
								} catch (java.text.ParseException e) {
									// TODO 自動生成された catch ブロック
									e.printStackTrace();
									return;
								}
							} catch (ParseException e) {
								// 未入力や日付の書式が異なる場合はなしとする
								publication_date = null;
							}
						} else if ("price".equals(nodeName)) {
							String price_str = prop.getFirstChild()
									.getNodeValue();
							price = Integer.parseInt(price_str);
						} else if ("lending".equals(nodeName)) {
							String lending_str = prop.getFirstChild()
									.getNodeValue();
							lending = Boolean.parseBoolean(lending_str);
						} else if ("key".equals(nodeName)) {
							key = prop.getFirstChild().getNodeValue();
							;
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
					book = b;
				}
				textDraw(book);
				Toast.makeText(this.activity, "成功", 0).show();
			}
		}

	}

	public class RequestImageTask extends AsyncTask<String, Integer, Drawable> {

		ImageView imageView;
		Activity activity;

		public RequestImageTask(Activity activity, ImageView imageView) {
			this.activity = activity;
			this.imageView = imageView;
		}

		@Override
		protected Drawable doInBackground(String... params) {
			String url = params[0];
			Drawable d = getCacheImage(url);
			if (d != null)
				return d;

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

		@Override
		protected void onPostExecute(Drawable image) {
			bookDetailImage.setImageDrawable(image);
		}

		private Object fetch(String address) throws MalformedURLException,
				IOException {
			URL url = new URL(address);
			Object content = url.getContent();
			return content;
		}

		private Drawable getCacheImage(String url) {
			if (cache.containsKey(url)) {
				Log.d("cache", "cache hit!");
				return cache.get(url);
			}
			return null;
		}

		private void cacheImage(String url, Drawable image) {
			cache.put(url, image);
		}

	}

}
