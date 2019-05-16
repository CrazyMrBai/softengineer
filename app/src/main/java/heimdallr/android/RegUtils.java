package heimdallr.android;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import pr.platerecognization.PlateRecognition;

public class RegUtils {

    private long handle;
    private Context context;



    public RegUtils(Context context) {

        this.context = context;

        this.initRecognizer();
    }


    //解析图片
    public static Bitmap decodeImage(String path) {
        Bitmap res;
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inSampleSize = 1;
            op.inJustDecodeBounds = false;
            //op.inMutable = true;
            res = BitmapFactory.decodeFile(path, op);
            //rotate and scale.
            Matrix matrix = new Matrix();

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }

            Bitmap temp = Bitmap.createBitmap(res, 0, 0, res.getWidth(), res.getHeight(), matrix, true);
            Log.d("com.arcsoft", "check target Image:" + temp.getWidth() + "X" + temp.getHeight());

            if (!temp.equals(res)) {
                res.recycle();
            }
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    //旋转角度
    public Bitmap roatePic(Bitmap bmp , int degree){
        if (degree == 0 || null == bmp) return bmp;
        Bitmap returnBm = null;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
        returnBm = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        if (returnBm == null) {
            returnBm = bmp;
        }
        if (returnBm != bmp && !bmp.isRecycled()) {
            bmp.recycle();
            bmp = null;
        }

        return returnBm;
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor = context.getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }

        return path;
    }

    /**
     * 4.4及以上的系统使用这个方法处理图片
     *
     * @param data
     */
    @TargetApi(19)
    public  void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(context, uri)) {
            //如果document类型的Uri,则通过document来处理
            String docID = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docID.split(":")[1];     //解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;

                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/piblic_downloads"), Long.valueOf(docID));

                imagePath = getImagePath(contentUri, null);

            }

        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的uri，则使用普通方式使用
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型的uri，直接获取路径即可
            imagePath = uri.getPath();

        }

    }


    public void copyFilesFromAssets(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                // directory
                File file = new File(newPath);
                if (!file.mkdir())
                {
                    Log.d("mkdir","can't make folder");

                }
//                    return false;                // copy recursively
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, oldPath + "/" + fileName,
                            newPath + "/" + fileName);
                }
            } else {
                // file
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
    }

    public void initRecognizer()
    {
        String assetPath = "pr";
        String sdcardPath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        copyFilesFromAssets(context, assetPath, sdcardPath);
        String cascade_filename  =  sdcardPath
                + File.separator+"cascade.xml";
        String finemapping_prototxt  =  sdcardPath
                + File.separator+"HorizonalFinemapping.prototxt";
        String finemapping_caffemodel  =  sdcardPath
                + File.separator+"HorizonalFinemapping.caffemodel";
        String segmentation_prototxt =  sdcardPath
                + File.separator+"Segmentation.prototxt";
        String segmentation_caffemodel =  sdcardPath
                + File.separator+"Segmentation.caffemodel";
        String character_prototxt =  sdcardPath
                + File.separator+"CharacterRecognization.prototxt";
        String character_caffemodel=  sdcardPath
                + File.separator+"CharacterRecognization.caffemodel";
        handle  =  PlateRecognition.InitPlateRecognizer(
                cascade_filename,
                finemapping_prototxt,finemapping_caffemodel,
                segmentation_prototxt,segmentation_caffemodel,
                character_prototxt,character_caffemodel
        );

    }
    public  String simpleRecog(Bitmap bmp)
    {
        //ast.makeText(VideoActivity.this,"11",Toast.LENGTH_LONG).show();
        Log.d("MainmenuActivity","try_reg0");
        float dp_asp  = 0.1f;
//        Mat mat_src = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC1);

        Log.d("MainmenuActivity","bmp" + bmp.getWidth()+"  "+bmp.getHeight());
        Mat mat_src = new Mat(5, 5, CvType.CV_8UC4);
        Log.d("MainmenuActivity","try_reg01");
        float new_w = bmp.getWidth()*dp_asp;
        float new_h = bmp.getHeight()*dp_asp;
        Size sz = new Size(new_w,new_h);
        Log.d("MainmenuActivity","try_reg1");
        Utils.bitmapToMat(bmp, mat_src);
        Log.d("MainmenuActivity","try_reg2");
        Imgproc.resize(mat_src,mat_src,sz);
        Log.d("MainmenuActivity","try_reg3");
        long currentTime1 = System.currentTimeMillis();
        Log.d("MainmenuActivity","try_reg4");
        String res = PlateRecognition.SimpleRecognization(mat_src.getNativeObjAddr(),handle);
        Log.d("MainmenuActivity","try_reg5");
        long diff = System.currentTimeMillis() - currentTime1;
        return res + String.valueOf(diff)+"ms";

    }


}
