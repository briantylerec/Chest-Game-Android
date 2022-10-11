package com.monksoft.chestgame

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TableRow
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot.capture
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.monksoft.chestgame.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    private var bitmap: Bitmap? = null

    private var mHandler: Handler? = null
    private var timeSeconds: Long = 0
    private var width_bonus = 0
    private var gaming = true
    private var string_share = ""

    private var nextLevel = false

    private var cellSelected_x = 0
    private var cellSelected_y = 0

    private var level = 1
    private var lives = 1
    private var levelMoves = 0
    private var movesRequired = 0
    private var moves = 0
    private var options = 0
    private var bonus = 0
    private var score_lives = 1
    private var score_level = 1

    private var unloaded: Boolean = false

    private var checkMovement = true

    private var nameColorBlack = "black_cell"
    private var nameColorWhite = "white_cell"


    private lateinit var board: Array<IntArray>
    lateinit var adView : AdView
    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAds()
        initScreenGame()
        startGame()
    }

    private fun initAds(){
        MobileAds.initialize(this) {}

        adView = AdView(this)
        adView.setAdSize( AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-3940256099942544~3347511713"

        val adRequest = AdRequest.Builder().build()
        binding.adView.addView(adView)
        binding.adView.loadAd(adRequest)
    }

    private fun showInterstitial(){
        if (mInterstitialAd != null) {
            unloaded = true

            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    //Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    //Log.d(TAG, "Ad dismissed fullscreen content.")
                    mInterstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    //Log.e(TAG, "Ad failed to show fullscreen content.")
                    mInterstitialAd = null
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    //Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    //Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
            mInterstitialAd?.show(this)
        }
    }

    private fun getReadyAds(){
        var adRequest = AdRequest.Builder().build()
        unloaded = false

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("TAG", adError.toString())
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("TAG", "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })
    }

    fun checkCellClicked(v: View){
        var name = v.tag.toString()
        var x = name.subSequence(1,2).toString().toInt()
        var y = name.subSequence(2,3).toString().toInt()
        checkCell(x,y)
    }

    private fun checkCell(x: Int, y: Int){
        var dif_x = x - cellSelected_x
        var dif_y = y - cellSelected_y
        var checkTrue = false

        if ((dif_x == 1 && dif_y == 2)
            || (dif_x == 1 && dif_y == -2) || (dif_x == 2 && dif_y == 1) || (dif_x == 2 && dif_y == -1)
            || (dif_x == -1 && dif_y == 2) || (dif_x == -1 && dif_y == -2) || (dif_x == -2 && dif_y == 1)
            || (dif_x == -2 && dif_y == -1) || (board[x][y] == 1 )) checkTrue = false

        if(board[x][y] == 1) checkTrue = false

        if(checkTrue) selectCell(x,y)
    }

    private fun resetBoard(){
        // 0 Esta libre.
        // 1 Casilla marcada.
        // 2 Es un bonus.
        // 9 Es una opcion del movimiento actual.
        board = Array(9) {IntArray(8) {0}}
    }

    private fun setFirstPosition(){

        var x =0
        var y = 0
        var firstPosition = false
        while(!firstPosition){
            x = (0..7).random()
            y = (0..7).random()
            if(board[x][y] == 0) firstPosition = true
            checkOptions(x,y)
            if(options == 0) firstPosition = false
        }
        cellSelected_x = x
        cellSelected_y = y
        selectCell(cellSelected_x, cellSelected_y)
    }

    private fun selectCell(x: Int, y: Int){
        moves --
        binding.tvMovesData.text = moves.toString()
        growProgressBonus()

        if (board[x][y] == 2){
            bonus++
            binding.tvBonusData.text = " + $bonus"
        }
        board[x][y] = 1
        paintHorseCell(cellSelected_x, cellSelected_y, "previus_cell")
        cellSelected_x = x
        cellSelected_y = y
        clearOptions()
        paintHorseCell(x,y, "selected_cell")
        checkMovement = true
        checkOptions(x,y)
        if (moves >= 0){
            checkNewBonus()
            checkGameOver()
        } else showMessage("You win!!", "Next Level", false)
    }

    private fun checkGameOver() {
        if(options == 0){
            if (bonus>0){
                checkMovement = false
                paintAllOptions()
            }
            else showMessage("Game Over!!", "Try Again", false)
        }
    }

    private fun paintAllOptions() {
        for (i in 0..7){
            for (j in 0..7){
                if(board[i][j] != 1) paintOptions(i,j)
                if(board[i][j] == 0) board[i][j] = 9
            }
        }
    }

    private fun showMessage(tittle: String, action: String, gameOver: Boolean) {

        gaming = false
        nextLevel = !gameOver

        binding.lyMessage.visibility = View.VISIBLE
        binding.tvTitleMessage.text = tittle
        binding.tvTimeData

        var score = binding.tvTimeData.text.toString()
        if (gameOver) {
            showInterstitial()
            score = "Score: $levelMoves-$moves / $levelMoves"
            string_share = "This game make me sick!!! "+ score +" Get it on -> http://monksoft.com"
        }
        else string_share = "Let's go!!! New challenge completed. Level: $level ("+ score +") http://monksoft.com"

        binding.tvScoreMessage.text = score

        binding.tvAction.text = action

    }

    private fun growProgressBonus(){
        var moves_done = levelMoves - moves
        var bonus_done = moves_done / movesRequired
        var moves_rest = movesRequired * (bonus_done)
        var bonus_grow = moves_done - moves_rest

        var v = findViewById<View>(R.id.vNewBonus)
        var widthBonus = ((width_bonus/movesRequired) * bonus_grow).toFloat()
        var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, getResources().getDisplayMetrics()).toInt()
        var width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthBonus, getResources().getDisplayMetrics()).toInt()

        v.setLayoutParams(TableRow.LayoutParams(width, height))
    }

    private fun checkNewBonus(){
        if(moves % movesRequired == 0){
            var bonusCell_x = 0
            var bonusCell_y = 0
            var bonusCell = false
            while(!bonusCell){
                bonusCell_x = (0..7).random()
                bonusCell_y = (0..7).random()
                if(board[bonusCell_x][bonusCell_y] == 0){
                    bonusCell = true
                }
            }
            board[bonusCell_x][bonusCell_y] = 2
            paintBonusCell(bonusCell_x,bonusCell_y)
        }
    }

    private fun paintBonusCell(x: Int, y:Int){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y","id", packageName))
        iv.setImageResource(R.drawable.bonus)
    }

    private fun clearOptions(){
        for (i in 0..7){
            for (j in 0..7){
                if(board[i][j] == 9 || board[i][j] == 2){
                    if (board[i][j] == 9) board[i][j] == 0
                    clearOption(i,j)
                }
            }
        }
    }

    private fun clearOption(x: Int, y: Int){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        if (checkColorCell(x,y) == "black")
            iv.setBackgroundColor(ContextCompat.getColor(this, resources.getIdentifier(nameColorBlack, "color", packageName)))
        else iv.setBackgroundColor(ContextCompat.getColor(this, resources.getIdentifier(nameColorWhite, "color", packageName)))

        if(board[x][y] == 1) iv.setBackgroundColor(ContextCompat.getColor(this, resources.getIdentifier("previus_cell", "color", packageName)))
    }

    private fun checkOptions(x: Int, y: Int){
        options = 0
        for(i in -2..2) {
            for(j in -2..2) {
                if(i!=0 && j!=0 && abs(i)!=abs(j)) checkMove(x, y, i, j)
            }
        }
        binding.tvOptionsData.text = options.toString()
    }

    private fun checkMove(x: Int, y: Int, mov_x: Int, mov_y: Int){
        var option_x = x+mov_x
        var option_y = y+mov_y
        if (option_x < 8 && option_y < 8 && option_x >= 0 && option_y >= 0){
            if(board[option_x][option_y] == 0 || board[option_x][option_y] == 2 ){
                options++
                paintOptions(option_x, option_y)
                if (board[option_x][option_y] == 0) board[option_x][option_y] = 9
            }
        }
    }

    private fun paintOptions(x: Int, y: Int){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        if(checkColorCell(x,y) == "black") iv.setBackgroundResource(R.drawable.option_black)
        else iv.setBackgroundResource(R.drawable.option_white)
    }

    private fun checkColorCell(x: Int, y: Int): String{
        var color = "white"
        var blackColumn_x = arrayOf(0,2,4,6)
        var blackRow_x = arrayOf(1,3,5,7)
        if((blackColumn_x.contains(x) && blackColumn_x.contains(y)) || (blackRow_x.contains(x) && blackRow_x.contains(y)))
            color = "black"

        return color
    }

    private fun paintHorseCell(x: Int, y: Int, color: String){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        iv.setBackgroundColor(ContextCompat.getColor(this, resources.getIdentifier(color,"color", packageName)))
        iv.setImageResource(R.drawable.icon)
    }

    private fun initScreenGame(){
        setSizeBoard()
        hideMessage(false)
    }

    private fun setSizeBoard(){
        var iv: ImageView
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x

        var width_dp = (width / resources.displayMetrics.density)

        var lateralMarginDp = 0
        val width_cell = (width_dp - lateralMarginDp)/8
        val height_cell = width_cell

        for(i in 0..7){
            for(j in 0..7){
                iv = findViewById(resources.getIdentifier("c$i$j","id", packageName))
                var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height_cell, resources.displayMetrics).toInt()
                var width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width_cell, resources.displayMetrics).toInt()

                iv.setLayoutParams(TableRow.LayoutParams(width, height))
            }
        }
    }

    private fun hideMessage(start: Boolean){
        binding.lyMessage.visibility = View.INVISIBLE

        if (start) startGame()
    }

    private fun launchShareGame(v:View){
        shareGame()
    }

    private fun launchAction(view : View){
        hideMessage(true)
    }

    private fun shareGame() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        var ssc: ScreenCapture = capture(this)
        bitmap = ssc.bitmap

        if(bitmap != null){
            var idGame = SimpleDateFormat("yyyy/MM/dd".format(Date())).toString()
            idGame = idGame.replace(":","")
            idGame = idGame.replace("/","")

            val path = saveImage(bitmap!!, "${idGame}.jpg")
            val bmpUri = Uri.parse(path)

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            shareIntent.putExtra(Intent.EXTRA_TEXT, string_share)
            shareIntent.type = "image/png"

            val finalShareIntent = Intent.createChooser(shareIntent, "Select the app you wanna share the game with!")
            finalShareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(finalShareIntent)
        }
    }

    private fun saveImage(bitmap: Bitmap, fileName: String): String? {
        if(bitmap == null) return null

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
            val contenValues = ContentValues().apply{
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots")
            }

            val uri = this.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contenValues)
            if (uri != null) {
                this.contentResolver.openOutputStream(uri).use {
                    if (it == null) return null

                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, it)
                    it.flush()
                    it.close()

                    MediaScannerConnection.scanFile(this, arrayOf(uri.toString()), null, null)
                }
            }
            return uri.toString()
        }

        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Screenshots"). absolutePath

        val dir = File(filePath)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        val FOut = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 85, FOut)
        FOut.flush()
        FOut.close()

        MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null, null)

        return filePath
    }

    private fun clearBoard() {
        var iv: ImageView

        var colorBlack = ContextCompat.getColor(this, resources.getIdentifier(nameColorBlack, "color", packageName))
        var colorWhite = ContextCompat.getColor(this, resources.getIdentifier(nameColorWhite, "color", packageName))

        for (i in 0..7) {
            for (j in 0..7) {
                iv = findViewById(resources.getIdentifier("c$i$j", "id", packageName))
                iv.setImageResource(0)

                if (checkColorCell(i, j) == "black") iv.setBackgroundColor(colorBlack)
                else iv.setBackgroundColor(colorWhite)
            }
        }
    }

    private fun resetTime() {
        mHandler?.removeCallbacks(chronometer)
        timeSeconds = 0
        binding.tvTimeData.text = "00:00"

    }

    private fun startTime() {
        mHandler = Handler(Looper.getMainLooper()!!)
        chronometer.run()
    }

    private var chronometer: Runnable = object: Runnable {
        override fun run() {
            try {
                if (gaming) {
                    timeSeconds++
                    updateStopWatchView(timeSeconds)
                }
            } finally {
                mHandler!!.postDelayed(this, 1000L)
            }
        }
    }

    private fun updateStopWatchView(timeInSeconds: Long) {
        val formattedTime = getFormattedStopWatch((timeInSeconds * 1000))
        binding.tvTimeData.text = formattedTime
    }

    private fun getFormattedStopWatch(ms: Long): String {
        var milliseconds = ms
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        return "${if (minutes < 10) "0" else ""}$minutes" + ":" +
                "${if (seconds < 10) "0" else ""}$seconds"
    }

    private fun startGame() {

        if (unloaded) getReadyAds()

        setLevel()
        setLevelParameters()

        resetBoard()
        clearBoard()

        setBoardLevel()
        setFirstPosition()

        resetTime()
        startTime()
        gaming = true
    }

    private fun setBoardLevel() {
        when(level){
            2 -> paintLevel_2()
            3 -> paintLevel_3()
            4 -> paintLevel_4()
            5 -> paintLevel_5()
            6 -> paintLevel_2()
            7 -> paintLevel_3()
            8 -> paintLevel_4()
            9 -> paintLevel_2()
            10 -> paintLevel_3()
            11 -> paintLevel_4()
            12 -> paintLevel_2()
            13 -> paintLevel_3()
            14 -> paintLevel_5()
        }
    }

    private fun pain_column(column: Int){
        for(i in 0..7){
            board[column][i] = 1
            paintHorseCell(column, 1, "previus_call")
        }
    }

    private fun paintLevel_2() {
        pain_column(6)
    }

    private fun paintLevel_3() {
        for(i in 0..7){
            for(j in 0..7){
                board[j][i] = 1
                paintHorseCell(j, i, "previus_call")
            }
        }
    }

    private fun paintLevel_4(){
        paintLevel_3()
        paintLevel_5()
    }

    private fun paintLevel_5(){
        for(i in 0..3){
            for(j in 0..3){
                board[j][i] = 1
                paintHorseCell(j, i, "previus_call")
            }
        }
    }

    private fun setLevelParameters() {
        binding.tvLiveData.text = lives.toString()

        score_lives = lives
        binding.tvLevelNumber.text = level.toString()
        score_level = level

        bonus = 0
        binding.tvBonusData.text = ""

        setLevelMoves()
        moves = levelMoves

        movesRequired = setMovesRequired()
    }

    private fun setMovesRequired(): Int {
        var movesRequired = 0

        when(level){
            1 -> movesRequired = 8
            2 -> movesRequired = 10
            3 -> movesRequired = 12
            4 -> movesRequired = 14
            5 -> movesRequired = 48
            6 -> movesRequired = 36
            7 -> movesRequired = 48
            8 -> movesRequired = 49
            9 -> movesRequired = 59
            10 -> movesRequired = 48
            11 -> movesRequired = 64
            12 -> movesRequired = 48
            13 -> movesRequired = 48
        }

        return movesRequired
    }

    private fun setLevelMoves() {
        when(level){
            1 -> levelMoves = 64
            2 -> levelMoves = 56
            3 -> levelMoves = 32
            4 -> levelMoves = 16
            5 -> levelMoves = 48
            6 -> levelMoves = 36
            7 -> levelMoves = 48
            8 -> levelMoves = 49
            9 -> levelMoves = 59
            10 -> levelMoves = 48
            11 -> levelMoves = 64
            12 -> levelMoves = 48
            13 -> levelMoves = 48
        }
    }

    private fun setLevel() {
        if(nextLevel) level++
        else {
            lives--
            if(lives>1){
                level = 1
                lives = 1
            }
        }
    }
}