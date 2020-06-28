package com.audiolemon.videogenerator

class LaunchTest(){

}
fun main(args: Array<String>){

    //LemonDBManager.close()
    LemonDBManager.removeTask("TSKQKG042422")
    LemonFileManager.deleteTaskDirectory("TSKQKG042422")

}