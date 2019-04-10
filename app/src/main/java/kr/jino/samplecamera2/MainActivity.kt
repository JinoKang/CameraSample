package kr.jino.samplecamera2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.FileProvider
import com.tedpark.tedpermission.rx2.TedRx2Permission
import com.theartofdev.edmodo.cropper.CropImage
import io.reactivex.disposables.CompositeDisposable
import kr.jino.samplecamera2.databinding.ActivityMainBinding
import java.io.File


class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {

    private val REQUEST_TAKE_PHOTO = 1000
    private val REQUEST_TAKE_ALBUM = 1001

    private lateinit var photoFile: File
    private var isGranted: Boolean = false
    private lateinit var compositeDisposable: CompositeDisposable

    private fun requestPermission() {
        compositeDisposable = CompositeDisposable()
        compositeDisposable.add(TedRx2Permission.with(this)
                .setRationaleTitle("요청")
                .setRationaleMessage("제공하는 서비스를 정상적으로 사용하시려면 카메라와 저장공간 권한이 필요합니다.") // "we need permission for read contact and find your location"
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .request()
                .subscribe({ tedPermissionResult ->
                    if (tedPermissionResult.isGranted) {
                        isGranted = true
                        Toast.makeText(this, "권한이 승인되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "권한을 승인하지 않으시면 서비스 사용이 불가능합니다.", Toast.LENGTH_SHORT).show()
                        return@subscribe
                    }
                }, { throwable -> }))
    }

    @RequiresPermission(allOf = [Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA])
    override fun initView() {
        requestPermission()
        dataBinding.btTakePicture.setOnClickListener {
            if (isGranted) {
                takePicture()
                compositeDisposable.clear()
            } else {
                requestPermission()
            }
        }

        dataBinding.btTakeAlbum.setOnClickListener {
            if (isGranted) {
                takeAlbum()
                compositeDisposable.clear()
            } else {
                requestPermission()
            }
        }
    }

    override fun start() {

    }


    private fun takePicture() {
        val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        photoFile = createImageFile()

        if (takePicture.resolveActivity(packageManager) != null) {
            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", photoFile))
            startActivityForResult(takePicture, REQUEST_TAKE_PHOTO)
        }
    }

    private fun takeAlbum() {
        Intent(Intent.ACTION_PICK).apply {
            type = MediaStore.Images.Media.CONTENT_TYPE
            startActivityForResult(this, REQUEST_TAKE_ALBUM)
        }
    }

    private fun createImageFile(): File {
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdir()
        }
        val timeStamp = System.currentTimeMillis().toString()

        return File(mediaStorageDir.path + File.separator + "IMG_" + timeStamp + ".jpg")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_TAKE_PHOTO -> {
                if (resultCode == Activity.RESULT_OK) {
                    CropImage.activity(FileProvider.getUriForFile(baseContext, BuildConfig.APPLICATION_ID + ".provider", photoFile))
                            .start(this)
                }
            }

            REQUEST_TAKE_ALBUM -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.let {
                        val cursor = contentResolver.query(it.data, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                        cursor.moveToFirst()

                        photoFile = File(cursor.getString(columnIndex))
                        CropImage.activity(Uri.fromFile(photoFile))
                                .start(this)
                    }
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == Activity.RESULT_OK) {
                    val resultUri = result.uri
                    dataBinding.ivImage.setImageURI(resultUri)
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    val error = result.error
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


    }
}