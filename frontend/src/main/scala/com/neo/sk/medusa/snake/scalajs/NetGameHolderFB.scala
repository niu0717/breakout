package com.neo.sk.medusa.snake.scalajs

import com.neo.sk.medusa.snake.Breakout.{BoundaryFB, PointFB}
import com.neo.sk.medusa.snake.ProtocolFB
import com.neo.sk.medusa.snake.ProtocolFB._
import com.neo.sk.medusa.snake.scalajs.utils.JsFunc
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Button, Canvas, Div, Input}
import org.scalajs.dom.raw._
import org.seekloud.byteobject.{MiddleBufferInJs, decoder}
import org.seekloud.byteobject.ByteObject._

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

object NetGameHolderFB extends js.JSApp{

  val bounds = PointFB(BoundaryFB.h, BoundaryFB.w)
  val canvasUnit = 10
  val canvasBoundary = bounds * canvasUnit
  val sendBuffer = new MiddleBufferInJs(40960) //sender buffer
  var state = ""
  var webSocUp = false
  val grid = new GridOnWeb()
  var keyCount1 = 1
  var keyCountB = 1
  var ani = 0
  var isSync = false
  var end = false
  var level = 1
  var score = 0
  var score1 = 0
  var getBallDetailsTimer:Int = 0
  var watcherTimer:Int = 0
  var emoji:Int = 0
  var emojiLife = 0L
  var emojiS = 0L
  var emojiE = 0L
  var nowPlayer = ""
  var deadUser = 0
  var msgCount = 1
  var oneLeft = ""
  var adm = 0
  val watchKeys = Set(
    KeyCode.Space, KeyCode.Left, KeyCode.Right ,KeyCode.Q
  )
  val colorArr=Array("Olive","OliveDrab","Orange","OrangeRed","Orchid","PaleGoldenRod","PaleGreen","PaleTurquoise","PaleVioletRed","PapayaWhip","PeachPuff","Peru","Pink","Plum","PowderBlue","Purple","Red","RosyBrown","RoyalBlue","SaddleBrown","Salmon","SandyBrown","SeaGreen","SeaShell","Sienna","Silver","SkyBlue")
  private[this] val iDField = dom.document.getElementById("id").asInstanceOf[HTMLInputElement]
  private[this] val nameField = dom.document.getElementById("name").asInstanceOf[HTMLInputElement]
  private[this] val roomIdField = dom.document.getElementById("roomId").asInstanceOf[HTMLInputElement]
  private[this] val joinButton = dom.document.getElementById("join").asInstanceOf[HTMLButtonElement]
  private[this] val canvas = dom.document.getElementById("GameView1").asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  //图片
  private[this] val canvasPic = dom.document.getElementById("canvasPic").asInstanceOf[HTMLElement]
  private[this] val canvasPaddle = dom.document.getElementById("canvasPaddle").asInstanceOf[HTMLElement]
  private[this] val canvasBrick = dom.document.getElementById("canvasBrick").asInstanceOf[HTMLElement]
  private[this] val canvasBrick1 = dom.document.getElementById("canvasBrick1").asInstanceOf[HTMLElement]
  private[this] val canvasBrick2 = dom.document.getElementById("canvasBrick2").asInstanceOf[HTMLElement]
  private[this] val canvasBrick3 = dom.document.getElementById("canvasBrick3").asInstanceOf[HTMLElement]
  private[this] val redDiamond = dom.document.getElementById("red_diamond").asInstanceOf[HTMLElement]
  private[this] val paddleBlu = dom.document.getElementById("paddleBlu").asInstanceOf[HTMLElement]
  private[this] val paddleRed = dom.document.getElementById("paddleRed").asInstanceOf[HTMLElement]
  private[this] val canvasBall = dom.document.getElementById("canvasBall").asInstanceOf[HTMLElement]
  //登陆
  val userName = dom.document.getElementById("userName").asInstanceOf[Input]
  val password = dom.document.getElementById("password").asInstanceOf[Input]
  val loginBtn = dom.document.getElementById("loginButton").asInstanceOf[Button]
  val adBtn = dom.document.getElementById("adminButton").asInstanceOf[Button]
  //  val createRoomBtn = dom.document.getElementById("createRoom").asInstanceOf[Button]
  val mask = dom.document.getElementById("mask").asInstanceOf[Div]
  val loginArea = dom.document.getElementById("loginArea").asInstanceOf[Div]
  val loginL = dom.document.getElementById("loginL").asInstanceOf[Div]
  val registerInfo = dom.document.getElementById("registerInfo").asInstanceOf[Div]
  val trueBody = dom.document.getElementById("trueBody").asInstanceOf[Div]
  val sendBtn = dom.document.getElementById("send").asInstanceOf[HTMLButtonElement]
  var barrage =  dom.document.getElementById("msgDan").asInstanceOf[HTMLInputElement]
  //注册
  val ruserName = dom.document.getElementById("ruserName").asInstanceOf[Input]
  val rpassword = dom.document.getElementById("rpassword").asInstanceOf[Input]
  val rcomplete = dom.document.getElementById("complete").asInstanceOf[Button]
//区域
  val gameAr = dom.document.getElementById("game").asInstanceOf[Div]
  val adAr = dom.document.getElementById("admin").asInstanceOf[Div]
  val body = dom.document.getElementById("body").asInstanceOf[HTMLBodyElement]
  import scala.concurrent.ExecutionContext.Implicits.global

  val brickColorArr = Array(canvasBrick,canvasBrick1,canvasBrick2,canvasBrick3)
  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {
    drawGameOff()
    canvas.width = 850
    canvas.height = 661
    loginBtn.onclick={_ =>
      if(userName.value.length !=0 && !userName.value.equals("") && password.value.length !=0 && !password.value.equals("")){
        ConnectBackend.login(userName.value,password.value).map{ l=>
          if(l==1){
            mask.setAttribute("style","display:none")
            loginArea.setAttribute("style","display:none")
            trueBody.setAttribute("style","display:block")
          }else{
            JsFunc.alert("用户名或密码错误")
          }
        }
      }
    }
    adBtn.onclick ={_=>
      gameAr.setAttribute("style","display:none")
      adAr.setAttribute("style","display:block")
      body.removeAttribute("background")
      adm = 1
    }

    rcomplete.onclick={(event: MouseEvent) =>
      ConnectBackend.registerInfoF(ruserName.value,rpassword.value)
      event.preventDefault()
    }



    joinButton.onclick = { (event: MouseEvent) =>
      val username= userName.value
      val name = password.value
      val roomId = roomIdField.value
      val gameStream = new WebSocket(getWebSocketUri(dom.document, username,name,roomId))
      println(gameStream.url)
      gameStream.onopen = { (event0: Event) =>
        webSocUp = true
        drawGameOn()
        drawText("游戏开始")
//        playground.insertBefore(p("Game connection was successful!"), playground.firstChild)
        canvas.focus()
        canvas.onkeydown = {
          (e: dom.KeyboardEvent) => {
//                      println(s"keydown: ${e.keyCode}")
            if (watchKeys.contains(e.keyCode)) {
              if((state==""||state=="settlement") && roomId!="" && roomId !=null){
                if(e.keyCode == KeyCode.Space){
                  val msg: ProtocolFB.UserAction = JoinGame(username,name, roomId)
                  msg.fillMiddleBuffer(sendBuffer) //encode msg
                  val ab: ArrayBuffer = sendBuffer.result() //get encoded data.
                  gameStream.send(ab) // send data.
                }
                deadUser = 0
                score1 = 0
                score = 0
//                grid.updateModel()
              }


             if(state.equals("play")){
                if(keyCount1==1){
                  val msg:ProtocolFB.UserAction = Key(username,e.keyCode,100,1)
                  msg.fillMiddleBuffer(sendBuffer)
                  val ab1:ArrayBuffer = sendBuffer.result()
                  gameStream.send(ab1)
                }else{
                  val msg:ProtocolFB.UserAction = Key(username,e.keyCode,grid.actionMap(username)._2,keyCount1)
                  msg.fillMiddleBuffer(sendBuffer)
                  val ab2:ArrayBuffer = sendBuffer.result()
                  gameStream.send(ab2)
                }
              }
              //TODO send key
              e.preventDefault()
            }
          }

        }
        event0
      }
      joinGame(username,password.value,roomIdField.value,gameStream)
      event.preventDefault()
    }


    grid.brickInit(1)

    ani = dom.window.requestAnimationFrame(drawLoop())
  }

  def drawGameOn(): Unit = {
    ctx.drawImage(canvasPic, 0, 0,canvas.width, canvas.height)
  }

  def drawGameOff(): Unit = {
    ctx.drawImage(canvasPic, 0, 0, canvas.width, canvas.height)
  }
//  ; val pr = 9;
  var pw = 195; var ph = 20; var py = 622; var ps = 6

  def drawPaddle(keyCode:Int,pxx:Int,KeyCount:Int)={

    var px = pxx

    if(keyCode==39 && px<552 / 2 +195-(pw/2)){
      px += ps
    }else if(keyCode==37 && px > -pw/2){
      px += -ps
    }
//    if(keyCode == 81){
//      emojiE = 1
//      emojiS = System.currentTimeMillis()
//      ctx.drawImage(canvasBall,px,py-46)
//      ctx.drawImage(canvasPaddle,px,py)
//    }else
//     if(emojiS!=0 && System.currentTimeMillis() - emojiS <10000){
//       ctx.drawImage(canvasPaddle,px,py)
//      }else{
//       emojiS=0
//       pw = 195
//       ph = 20
       ctx.drawImage(canvasPaddle,px,py)
//     }
    if(grid.actionMap.nonEmpty && !state.equals("watch")){
      grid.updateActionMap(grid.actionMap.head._1,keyCode,px,KeyCount)
    }
  }

//  val br = 10
//  ; var bx = 120.0; var by = 120.0; var bsx = 1 + 0.4*1; var bsy = 1.5 - 0.4*1
  def drawBall(pxx:Int)={

    var bx = grid.ballInfo.x
    var by = grid.ballInfo.y
    var bsx = grid.ballInfo.sx
    var bsy = grid.ballInfo.sy
    if(state.equals("play")){
      var px = pxx

      if(by < 1){
        by = 1
        bsy =  -bsy
      }else if(by > canvas.height){
        bsx = 0;bsy = 0
        bx = 1000; by = 1000
        //      dom.window.cancelAnimationFrame(ani)
//        drawText("游戏结束")
        end = true
      }
      if(bx < 1){
        bx = 1
        bsx = -bsx
      }else if(bx > (552 / 2 +195)){
//        bx = canvas.width -
        bx = (552 / 2 +195)-1
        bsx = -bsx
      }

      if(bx >= px && bx <= (pw + px) && by >= py-46 && by <= (py-46+ph)){
        bsx = (7 * ((bx - (px + pw / 2)) / pw)).ceil
        bsy =  -bsy
      }

      bx += bsx
      by += bsy
      if(emojiS!=0 && System.currentTimeMillis() - emojiS <10000){
        grid.updateBallInfo(bx,by,6,6)
      }else{
        grid.updateBallInfo(bx,by,bsx,bsy)
      }
      ctx.drawImage(canvasBall,bx,by)
    }else{
      if(by > canvas.height){
        drawText("游戏结束")
        end = true
      }
      ctx.drawImage(canvasBall,bx,by)
    }

  }

  def drawText(text:String)={
    ctx.fillStyle = "black"
    ctx.fillRect(0,0,canvas.width,canvas.height)

    ctx.fillStyle = "yellow"
    ctx.textAlign = "center"
    ctx.font = "40px helvetica,arial"
    ctx.fillText(text,canvas.width / 2,canvas.height / 2)
  }
  val gap = 2; val w = 98; val h = 34; var row = 3; var total = 0; var i=0; var j =0

  var nextAnimation = 0.0 //保存requestAnimationFrame的ID

  def drawLoop(): Double => Unit = { _ =>
    nextAnimation = dom.window.requestAnimationFrame(drawLoop())
    if(state.equals("play") || state.equals("watch")|| state.equals("settlement")){

      drawGrid()
    }

  }

  def drawGrid()={
    ctx.clearRect(0,0,canvas.width,canvas.height)
    if(oneLeft == ""){
      if(deadUser == 2 ){
        if(score1>score){
          drawText("游戏结束,玩家1获胜")
        }else if(score1<score){
          drawText("游戏结束,玩家2获胜")
        }else{
          drawText("游戏结束,玩家1,2平局")
        }
      }else{
        ctx.drawImage(canvasPic, 0, 0, canvas.width, canvas.height)
        ctx.fillStyle = "white"
        ctx.font = "40px 华文琥珀"
        ctx.fillText("SCORE:" + score, 681, 144)
        ctx.fillText("LEVEL:" + level, 681, 220)
        ctx.fillText("PLAYER:" + nowPlayer, 681, 296)
        var bx = grid.ballInfo.x
        var by = grid.ballInfo.y
        var bsx = grid.ballInfo.sx
        var bsy = grid.ballInfo.sy
        for(i <- 0 until row){
          for(j <- 0 to 4){
            if(grid.brick(i)(j)!=1){
              //          if(bx >= (j*w+j*gap)-46 && bx <= (j*w+j*gap + w)+46 && by >= (i*h+i*gap)-46 && by <=(i*h+i*gap+h)+46){
              if(bx > (j*98+30-46) && bx <= j*98+46+30 && by > i*40 && by <= i*40+46 ){
                grid.updateBrickInfo(i,j)
                if(i==1 && j==2){
                  createTools = 1
                  tx = j*98+30
                  ty = 3*30+30
                }
                total += 1
                score += 1
                bsy = -bsy
                grid.updateBallInfo(bx,by,bsx,bsy)
              }
              val bricki = (Math.random()*3).toInt
              ctx.drawImage(brickColorArr(bricki),j*98+30,i*30+30)
              //          ctx.fillStyle = "red"
              //          ctx.fillRect((j*w+j*gap),(i*h+i*gap),w,h)
            }
          }
        }
        if(total == row * 5){
          level += 1
          row = row+1
          grid.brickInit(level)
          total = 0
        }
        if(isSync){
          if(grid.actionMap.nonEmpty){
            val id = grid.actionMap.head._1
            if(keyCountB == grid.actionMap(id)._3){
              drawPaddle(1,grid.actionMap(id)._2,grid.actionMap(id)._3)
            }else{
              drawPaddle(grid.actionMap(id)._1,grid.actionMap(id)._2,grid.actionMap(id)._3)
            }
            drawBall(grid.actionMap(id)._2)
            keyCountB = grid.actionMap(id)._3
          }

        }
        if(grid.barrageInfo.nonEmpty) {
          grid.barrageInfo.foreach{ barrage =>
            ctx.fillStyle = colorArr(barrage._2._3)
            ctx.font = "40px helvetica,arial"
            ctx.fillText(barrage._1,barrage._2._1-mx,barrage._2._2)
            if(barrage._2._1<0){
              grid.barrageInfo.remove(barrage._1)
            }else{
              grid.barrageInfo.update(barrage._1,(barrage._2._1-mx,barrage._2._2,barrage._2._3))
            }
          }
        }
      }
      if(createTools == 1){
        drawTools(tx,ty)
//        println("ty: "+ty)
      }
    }else{
      drawText(oneLeft)
    }

  }

//  var bax = (1.1 / Math.random()) * canvas.width
//  var bay = 0.5 * canvas.height * Math.random() + 36
  var mx = 8 + Math.random() * 6
//  def showBarrage()= {
//
//      grid.barrageInfo.foreach{ barrage =>
//        ctx.fillStyle = colorArr((Math.random()*26).toInt)
//        ctx.font = "40px helvetica,arial"
//        ctx.fillText(barrage._1,barrage._2._1-mx,barrage._2._2)
//        if(barrage._2._1<0){
//          grid.barrageInfo.remove(barrage._1)
//        }else{
//          grid.barrageInfo.update(barrage._1,(barrage._2._1-mx,barrage._2._2))
//        }
//      }
//
//  }

  var tx = 0;var ty = 0.0
  var createTools = 0;var ts = 5
  def drawTools(txx:Double,tyy:Double)={
    ctx.drawImage(redDiamond,txx,tyy)
    ty = tyy + ts
//    println(ty+"  "+py+" ;;;;; "+tx+" "+grid.actionMap.head._2._1)
    if(tx >= grid.actionMap.head._2._1 && tx <= (pw + grid.actionMap.head._2._1) && ty >= py-48 && ty <= (py-48+ph)) {
      println("-----")
//      pw = 104
//      ph = 24
//      grid.updateBallInfo(grid.ballInfo.x,grid.ballInfo.y,5,5)
      emojiS = System.currentTimeMillis()
    }
    if(ty>canvas.height){
      createTools = 0
    }
  }


  def joinGame(username:String,name: String,roomId:String,gameStream:WebSocket): Unit = {
    joinButton.disabled = true
    val playground = dom.document.getElementById("playground")
    playground.innerHTML = s"Trying to join game as '$username'..."


    sendBtn.onclick ={(event: MouseEvent) =>
      val msgDan = barrage.value
      if(msgDan != "" && msgDan.nonEmpty){
        val msg:ProtocolFB.UserAction =SendBarrage(msgDan)
        msg.fillMiddleBuffer(sendBuffer)
        val ab2:ArrayBuffer = sendBuffer.result()
        gameStream.send(ab2)
      }
      event.preventDefault()
    }


    gameStream.onerror = { (event: ErrorEvent) =>
      drawGameOff()
      playground.insertBefore(p(s"Failed: code: ${event.colno}"), playground.firstChild)
      joinButton.disabled = false
//      nameField.focus()
    }
    watcherTimer = dom.window.setInterval(()=> {
      if (end) {
        end = false
        //                bx = 120.0;  by = 120.0;  bsx = 1 + 0.4*1;  bsy = 1.5 - 0.4*1
        level = 1
        total = 0
        keyCount1 = 1
        msgCount = 1
        grid.brickInit(level)
        //              ani = dom.window.requestAnimationFrame(drawLoop())
        println("---: "+grid.nowModel)
        grid.actionMap.clear()
        sendBtn.disabled = false
        grid.barrageInfo.clear()
        if(username == grid.nowModel.player1){
          nowPlayer = grid.nowModel.player2.toString
        }else{
          nowPlayer = username
        }
        if(deadUser == 1){
          deadUser = 2
          state = "settlement"
          joinButton.disabled = false
          grid.nowModel = ChangeModel("0",false,"0",false,0)
          pw = 195
          ph=20
          val msg: ProtocolFB.UserAction = BackInit()
          msg.fillMiddleBuffer(sendBuffer)
          val ab2: ArrayBuffer = sendBuffer.result()
          gameStream.send(ab2)
          dom.window.clearInterval(watcherTimer)
        }else{
          deadUser = 1
          score1 = score
          val msg: ProtocolFB.UserAction = ChangeModel(grid.nowModel.player1, false, grid.nowModel.player2, true,score)
          msg.fillMiddleBuffer(sendBuffer)
          val ab2: ArrayBuffer = sendBuffer.result()
          gameStream.send(ab2)
          score = 0
        }

      }
    },1000/60)


    gameStream.onmessage = { (event: MessageEvent) =>
      event.data match {
        case blobMsg: Blob =>
//          netInfoHandler.dataCounter += blobMsg.size
          val fr = new FileReader()
          fr.readAsArrayBuffer(blobMsg)
          fr.onloadend = { _: Event =>
            val buf = fr.result.asInstanceOf[ArrayBuffer] // read data from ws.
          //decode process.
          val middleDataInJs = new MiddleBufferInJs(buf) //put data into MiddleBuffer
          val encodedData: Either[decoder.DecoderFailure, ProtocolFB.GameMessageFB] =
            bytesDecode[ProtocolFB.GameMessageFB](middleDataInJs) // get encoded data.
            //            GameView.canvas.focus()
//            println(encodedData)
//            println(grid.actionMap)
            encodedData match {
              case Right(data) =>
                data match {
                  case ProtocolFB.NewBJoined(id, user) =>
                    writeToArea(s"$user joined!")
                  case ProtocolFB.WaitOther(msg) =>
                    state="wait"
                    drawText(msg)

                  case ProtocolFB.JoinGameSuccess(username,name,roomId)=>
                    println("---id: "+ JoinGameSuccess(username,name,roomId))
                    state = "play"
                    sendBtn.disabled = true
                    nowPlayer = username
                    end = false
                    //                bx = 120.0;  by = 120.0;  bsx = 1 + 0.4*1;  bsy = 1.5 - 0.4*1
                    level = 1
                    total = 0
                    score = 0
                    grid.brickInit(level)
                    getBallDetailsTimer =  dom.window.setInterval(()=>{
                      if(state.equals("watch") || state.equals("settlement")) {
                        dom.window.clearInterval(getBallDetailsTimer)
                      }
                      val msg: ProtocolFB.UserAction = GetBallDetails(grid.ballInfo.x, grid.ballInfo.y, grid.ballInfo.sx, grid.ballInfo.sy)
                      msg.fillMiddleBuffer(sendBuffer)
                      val ab1: ArrayBuffer = sendBuffer.result()
                      gameStream.send(ab1)
                    },1000/60)
                    isSync = true

                  case ProtocolFB.watchOther(msg) =>
                    state = "watch"
                    drawText(msg)
                    isSync = true

                  case ProtocolFB.PlayToWatch() =>
                    state = "watch"

                  case ProtocolFB.PaddleAction(playerName,keyCode,px,keyCount) =>
//                    println("pad:   "+playerId)
                    if(state.equals("play")){
                      grid.addActionMap(playerName,keyCode,px,keyCount)
                      keyCount1 += 1
                    }
//                    println(playerName+"          "+grid.actionMap)

                  case ProtocolFB.GridDataForWatch(watcher,watchInfo) =>
                    if(grid.actionMap.isEmpty){
                      grid.addActionMap(watchInfo.playerName,watchInfo.keyCode,watchInfo.px,watchInfo.KeyCount)
                    }else{
                      grid.updateActionMap(watchInfo.playerName,watchInfo.keyCode,watchInfo.px,watchInfo.KeyCount)
                    }

//                  case ProtocolFB.PlayerLife(myself,player1,player1Life,player2,player2Life) =>
//                    if(myself == player2 && !player1Life){
//                      state = "play"
//                    }else if(myself == player1 && player1Life){
//                      state = "watch"
//                    }

                  case ProtocolFB.SyncBallDetails(x,y,sx,sy) =>
                    grid.updateBallInfo(x,y,sx,sy)

                  case ProtocolFB.PlayerList(player1,player2) =>
                    println(player1+"  "+player2)
                    grid.playerList(0) = player1.toString
                    grid.playerList(1) = player2.toString
                    if(state.equals("watch") || state.equals("wait")){
                      if(username == grid.playerList(0)){
                        nowPlayer = grid.playerList(1)
                        grid.updateModel(nowPlayer,grid.playerList(0),false)
                      }else{
                        nowPlayer = grid.playerList(0)
                        grid.updateModel(nowPlayer,grid.playerList(1),false)
                      }

                    }else{
                      if(username == grid.playerList(0)){
                        grid.updateModel(username,grid.playerList(1),false)
                      }else{
                        grid.updateModel(username,grid.playerList(0),false)
                      }
                    }
                  case ProtocolFB.SyncBarrage(msg) =>
                    grid.barrageInfo.put(msgCount+":"+msg ,((1.1 / Math.random()) * canvas.width, 0.5 * canvas.height * Math.random() + 36,(Math.random()*26).toInt))
                    msgCount += 1
//                    println(grid.barrageInfo)

                  case ProtocolFB.OneLeft()=>
                    joinButton.disabled = false
                    oneLeft = "另一位玩家已经离开房间，你自动获胜"

                  case x =>
                    println("error: "+ x)

//                  case ProtocolFB.Score(player1) =>
//
//
//                  case data: ProtocolFB.GridDataMessage =>
//                    drawGrid(data)
                }
            }
          }
      }
    }


    gameStream.onclose = { (event: Event) =>
      drawGameOff()
      playground.insertBefore(p("Connection to game lost. You can try to rejoin manually."), playground.firstChild)
      joinButton.disabled = false
    }

    def writeToArea(text: String): Unit ={
      playground.insertBefore(p(text), playground.firstChild)
    }
  }


  def getWebSocketUri(document: Document,id:String, nameOfChatParticipant: String,roomId:String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/medusa/link/playGame?playerId=$id&playerName=$nameOfChatParticipant&roomId=$roomId"
  }

  def p(msg: String) = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = msg
    paragraph
  }

}
