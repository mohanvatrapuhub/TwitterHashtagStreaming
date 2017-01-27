package com.Spark.Streaming

import org.apache.spark.sql._
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.Seconds._

import twitter4j.auth.AuthorizationFactory
import twitter4j.conf.ConfigurationBuilder

import org.apache.log4j.{Level, Logger}

/**
  * Created by MohanChaitanyaReddyVatrapu on 1/26/17.
  */


object TwitterHashtags {
  //Building main method inside the object file
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
                      .master("local")
                        .appName("TwitterTweets")
                          .config("spark.cleaner.ttl", "3600")
                              .getOrCreate()

  //Setting up sparkContext and StreamingContext
    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(2))

    import spark.implicits._

    if (!Logger.getRootLogger.getAllAppenders.hasMoreElements) {
      Logger.getRootLogger.setLevel(Level.WARN)
    }

  //Setting up twitter authorization
    val consumerKey = " "
    val consumerSecret = " "
    val accessToken = " "
    val accessTokenSecret = " "

    val conf = new ConfigurationBuilder()
    conf.setOAuthAccessToken(accessToken)
    conf.setOAuthAccessTokenSecret(accessTokenSecret)
    conf.setOAuthConsumerKey(consumerKey)
    conf.setOAuthConsumerSecret(consumerSecret)

    val auth = Option(AuthorizationFactory.getInstance(conf.build()))

    val filter = args.takeRight(args.length - 4)

    val tStream = TwitterUtils.createStream(ssc, auth, filter)

    val hashTags = tStream.flatMap(a => a.getText.split(" ").filter(_.startsWith("#")))

    //Calculates popular hashtags with 50sec duration for every 2sec and prints top 5 hashtags
    val topCount50 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(60))
                        .map{case (a, b) => (a, b)}.transform(_.sortByKey(false))

    topCount50.foreachRDD(x => {
      val top = x.take(5)
      println("\nTop #Hashtags for 50 sec (%s total):" .format(x.count()))
      top.foreach{case (a, b) => print("%s (%s tweets)".format(b, a))}
    })

    ssc.start()
    ssc.awaitTermination()

  }

}
