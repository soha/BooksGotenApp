package jp.android.BooksGotenListView.jp.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jp.android.BooksGotenListView.R;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 書籍の詳細表示
 * @author you
 *
 */
public class BookDetailActivity extends Activity implements OnClickListener {

	TextView bookDetailText;
	ImageView bookDetailImage;
	Button lendingButton;
	Book book;
	private HashMap<String,Drawable> cache = new HashMap<String,Drawable>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detail);
		
		bookDetailText = (TextView)findViewById(R.id.BookDetailView);
		bookDetailImage = (ImageView)findViewById(R.id.BookImageDetailView);
		lendingButton = (Button)findViewById(R.id.LendingButton);

		lendingButton.setOnClickListener(this);
		
		Intent intent = getIntent();
		book = (Book) intent.getSerializableExtra("book");
		
		textDraw(book);

		//画像の取得・表示
		RequestImageTask task = new RequestImageTask(this, bookDetailImage);
		task.execute(book.image_url);
		
	}
	
	private void textDraw(Book book) {
		bookDetailText.setText(book.title);
		bookDetailText.setText(bookDetailText.getText() + "\n\n" + book.author + "\n" + book.publisher);
		bookDetailText.setText(bookDetailText.getText() + "\n" + book.price + "円");
		String lendingStr = "";
		if(book.lending) {
			lendingStr = "貸出中";
			lendingButton.setText("返却");
			
		}else{
			lendingStr = "棚に在り";
			lendingButton.setText("借りる");
		}
		bookDetailText.setText(bookDetailText.getText() + "\n\n" + lendingStr);
	}
	
	
	public void onClick(View v) {
		try {
			String lendUrlStr = "http://booksgoten.appspot.com/catalog/lend/" + book.key; 
			URL lendUrl = new URL(lendUrlStr);
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(lendUrlStr);
			List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
			nameValuePair.add(new BasicNameValuePair("lending", Boolean.toString(!book.lending)));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePair));
			httpclient.execute(httppost);
			
			lendUrl.getContent();
		} catch (MalformedURLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return;
		}
		book.lending = !book.lending;
		textDraw(book);
		
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
		
		@Override
		protected void onPostExecute(Drawable image) {
			bookDetailImage.setImageDrawable(image);
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
	        }  
	        return null;  
		}
		
		private void cacheImage(String url, Drawable image) {
			cache.put(url, image);
		}
		
	}




	
}
