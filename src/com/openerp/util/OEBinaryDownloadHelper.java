package com.openerp.util;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Base64;
import android.widget.Toast;

import com.openerp.MainActivity;
import com.openerp.orm.BaseDBHelper;
import com.openerp.orm.OEHelper;
import com.openerp.support.OpenERPServerConnection;

public class OEBinaryDownloadHelper {
	ProgressDialog mProgressDialog;
	DownloadTask downloadTask = null;

	public void downloadBinary(int id, BaseDBHelper db) {
		if (OpenERPServerConnection.isNetworkAvailable(MainActivity.context)) {
			mProgressDialog = new ProgressDialog(MainActivity.context);
			mProgressDialog.setMessage("Downloading...");
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setCancelable(true);

			downloadTask = new DownloadTask(db);
			downloadTask.execute(id);
		} else {
			Toast.makeText(MainActivity.context, "Unable to connect server !",
					Toast.LENGTH_LONG).show();
		}
	}

	private class DownloadTask extends AsyncTask<Integer, Integer, Boolean> {

		int attachment_id = 0;
		BaseDBHelper db = null;

		public DownloadTask(BaseDBHelper db) {
			this.db = db;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setProgress(values[0]);
		}

		@Override
		protected Boolean doInBackground(Integer... arg) {
			this.attachment_id = arg[0];
			OEHelper oe = db.getOEInstance();
			try {
				JSONObject fields = new JSONObject();
				fields.accumulate("fields", "db_datas");
				fields.accumulate("fields", "datas_fname");
				JSONObject res = oe.search_read("ir.attachment", fields,
						new JSONObject().accumulate("domain",
								new JSONArray("[[\"id\", \"=\", "
										+ this.attachment_id + "]]")), 0, 1,
						null, null);
				String base64Data = res.getJSONArray("records")
						.getJSONObject(0).getString("db_datas");
				String filename = res.getJSONArray("records").getJSONObject(0)
						.getString("datas_fname");
				int count;

				byte[] fileAsBytes = Base64.decode(base64Data, 0);
				int lenghtOfFile = fileAsBytes.length;
				InputStream is = new ByteArrayInputStream(fileAsBytes);
				filename = filename.replaceAll("[-+^:, ]", "_");
				String path = "/sdcard/Download/" + filename;
				FileOutputStream fos = new FileOutputStream(path);
				byte data[] = new byte[1024];
				long total = 0;
				int progress = 0;
				while ((count = is.read(data)) != -1) {
					total += count;
					int progress_temp = (int) total * 100 / lenghtOfFile;
					publishProgress(progress_temp);
					if (progress_temp % 10 == 0 && progress != progress_temp) {
						progress = progress_temp;
					}
					fos.write(data, 0, count);

				}
				is.close();
				fos.close();

			} catch (Exception e) {
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mProgressDialog.dismiss();
			if (!result) {
				Toast.makeText(MainActivity.context,
						"Download error: " + result, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(MainActivity.context, "File downloaded",
						Toast.LENGTH_SHORT).show();
			}
		}
	}
}