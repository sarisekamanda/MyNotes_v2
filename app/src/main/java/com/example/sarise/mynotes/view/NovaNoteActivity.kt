package com.example.sarise.mynotes.view

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.sarise.mynotes.R
import com.example.sarise.mynotes.model.Note
import kotlinx.android.synthetic.main.activity_nova_note.*

class NovaNoteActivity: AppCompatActivity() {

    private var image_uri : Uri? = null
    private var mCurrentPhotoPath: String = ""

      //var note: Note? = null
    lateinit var note: Note

    private val channelId = "com.example.sarise.mynotes"
    private var notificationManager: NotificationManager? = null


    companion object {

        private val REQUEST_IMAGE_GARELLY = 1000
        private val REQUEST_IMAGE_CAPTURE = 2000
        const val EXTRA_REPLY = "view.REPLY"
        const val EXTRA_DELETE = "view.Delete"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nova_note)

        notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

        // botão de voltar ativo no menu superior esquerdo
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fabAddImagem.setOnCreateContextMenuListener { menu, v, menuInfo ->
            menu.add(Menu.NONE, 1, Menu.NONE, "Escolher foto")
//          menu.add(Menu.NONE, 2, Menu.NONE, "Tirar foto")
        }

        val intent:Intent = intent
        try {
            // receber o objeto da intent
            this.note = intent.getSerializableExtra(EXTRA_REPLY) as Note
            // para cada item do formulário, adiciono o valor do atributo do objeto
            note.let {
                etTitulo.setText(note.titulo)
                etConteudo.setText(note.conteudo)
            }
        } catch (e: Exception){

        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_GARELLY)
    }

  private fun takePicture() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "nova imagem")
        values.put(MediaStore.Images.Media.DESCRIPTION, "imagem da camera")
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")

        image_uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if(intent.resolveActivity(packageManager) != null) {
            mCurrentPhotoPath = image_uri.toString()
            intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun getPermissionImageFromGallery(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED) {
                // permission denied
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                requestPermissions(permission, REQUEST_IMAGE_GARELLY)
            } else {
                // permission granted
                pickImageFromGallery()
            }
        }
        else{
            // system < M
            pickImageFromGallery()
        }
    }

    private fun getPermissionTakePicture(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED) {
                // permission denied
                val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permission, REQUEST_IMAGE_CAPTURE)
            } else {
                // permission granted
               // takePicture()
            }
        }
        else{
            // system < M
           // takePicture()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            REQUEST_IMAGE_GARELLY -> {
                if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) pickImageFromGallery()
                else Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
            }
            REQUEST_IMAGE_CAPTURE ->{
                if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) takePicture()
                else Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE_GARELLY){
            image_uri = data?.data
            imgNovaNote.setImageURI(image_uri)
        }

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE) imgNovaNote.setImageURI(image_uri)

    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            1 -> getPermissionImageFromGallery()
            2 -> getPermissionTakePicture()
        }
        return super.onContextItemSelected(item)
    }

    // ---- MENU ----

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_nova_nota, menu)
        try {
            note.let {
                // torna o botão do item do menu visivel
                val menuItem = menu?.findItem(R.id.menu_note_delete)
                menuItem?.isVisible = true
            }
        } catch (e:Exception){
            // torna o botão do item do menu invisivel
            val menuItem = menu?.findItem(R.id.menu_note_delete)
            menuItem?.isVisible = false
        }
        return true
    }



    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (item?.itemId == android.R.id.home) {
            finish()
            true
        }
        else if (item?.itemId == R.id.menu_note_save){
            if(etTitulo.text.isNullOrEmpty()) Toast.makeText(this,
                "Insira o título da nota",
                Toast.LENGTH_LONG).show()
            else {
                if(::note.isInitialized && note.id>0){

                    note.titulo =  etTitulo.text.toString()
                    note.conteudo = etConteudo.text.toString()

                    noticacao(channelId,note.titulo,"nota alterada")
                }else{
                    note = Note(
                        titulo = etTitulo.text.toString(),
                        conteudo = etConteudo.text.toString()



                    )
                    noticacao(channelId, note.titulo, "Nota criada")
                }

                val replyIntent = Intent()

                replyIntent.putExtra(EXTRA_REPLY, note)

                setResult(Activity.RESULT_OK, replyIntent)
            }
            finish()
            true
        }
        else if(item?.itemId == R.id.menu_note_delete){
            val replyIntent = Intent()
            replyIntent.putExtra(EXTRA_DELETE, note)
            setResult(Activity.RESULT_OK, replyIntent)

            finish()
            true
        }
        else{
            super.onOptionsItemSelected(item)
        }
    }







    fun noticacao(id: String, nome: String, descricao: String){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(id, nome, importance)
            channel.description = descricao
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
            channel.enableVibration(true)
            channel.vibrationPattern =
                    longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

            notificationManager?.createNotificationChannel(channel)
        }
    }

}