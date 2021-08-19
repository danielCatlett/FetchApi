package com.example.fetchapi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fetchapi.API.ApiObjectJsonItem
import com.example.fetchapi.adapters.RecyclerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception

const val BASE_URL = "https://fetch-hiring.s3.amazonaws.com"

class MainActivity : AppCompatActivity()
{
    lateinit var countDownTimer: CountDownTimer

    private var idList = mutableListOf<Int>()
    private var listIdList = mutableListOf<Int>()
    private var nameList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        makeApiRequest("")

        //On click listener for the search button
        //First we clear the list of any previous data,
        //then we run another api request with any search
        //requests the user may have. This functionality
        //doubles as a refresh button
        imageButton.setOnClickListener(View.OnClickListener
        {
            idList.clear()
            listIdList.clear()
            nameList.clear()
            val searchParameter = searchBarEditText.text.toString()
            makeApiRequest(searchParameter)
        })
    }

    //Animate a fade in from black when the app is opened
    private fun fadeInFromBlack()
    {
        blackScreen.animate().apply {
            alpha(0f)
            duration = 3000
        }.start()
    }

    //Call on the adapter to present the data we have collected
    private fun setUpRecyclerView()
    {
        objectList.layoutManager = LinearLayoutManager(applicationContext)
        objectList.adapter = RecyclerAdapter(idList, listIdList, nameList)
    }

    private fun addToList(id: Int, listId: Int, name: String)
    {
        idList.add(id)
        listIdList.add(listId)
        nameList.add(name)
    }

    //All logic for the pulling of data and then preparing it for the
    //RecyclerView is contained here
    private fun makeApiRequest(searchParameter: String)
    {
        progressBar.visibility = View.VISIBLE //start up the progress bar

        val api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiRequest::class.java)

        GlobalScope.launch(Dispatchers.IO)
        {
            try
            {
                val response = api.getObjects()
                val sortedResponse = sortData(response, searchParameter)

                for(apiObjectJsonItem in sortedResponse)
                {
                    addToList(apiObjectJsonItem.id, apiObjectJsonItem.listId, apiObjectJsonItem.name)
                }
                withContext(Dispatchers.Main)
                {
                    //upon successful data retrieval and sorting, call these functions
                    setUpRecyclerView()
                    fadeInFromBlack()
                    progressBar.visibility = View.GONE
                }
            }
            catch(e: Exception)
            {
                Log.e("MainActivity", e.toString())

                withContext(Dispatchers.Main)
                {
                    attemptRequestAgain()
                }
            }
        }
    }

    //This function handles any searches the user may have specified.
    //It looks for the string searched within the name value of each item,
    //and returns a list of only objects that have that string
    private fun filterResponses(response: ArrayList<ApiObjectJsonItem>, searchParameter: String) : ArrayList<ApiObjectJsonItem>
    {
        //If no search parameters have been specified, return the entire list
        if(searchParameter == "")
        {
            return response
        }
        var filteredResponse = ArrayList<ApiObjectJsonItem>()

        for(item in response)
        {
            if(item.name.contains(searchParameter))
            {
                filteredResponse.add(item)
            }
        }

        return filteredResponse
    }

    //Data comes to this function in a random order, and leaves in the required order,
    //Sorted first by listId, then by name, with all null values filtered out.
    private fun sortData(response: ArrayList<ApiObjectJsonItem>, searchParameter: String) : ArrayList<ApiObjectJsonItem>
    {
        //filter nulls
        var noNullItems = ArrayList<ApiObjectJsonItem>()
        for(item in response)
        {
            if(item.name != "" && item.name != null)
            {
                noNullItems.add(item)
            }
        }

        noNullItems = filterResponses(noNullItems, searchParameter)

        /*
        When I first sorted by name, I ran into the problem of the names being sorted by
        alphabetical order. This may have been okay, but since all of the names were actually
        Item then number, I ran into issues where we would go from "Item 599" to "Item 6" to
        "Item 601". This code block solves that issue by extracting a substring containing just
        the number at the end of the string, putting that number in a Pair with the object, and then
        building a new ArrayList of those created pairs. That way, the objects with the same listId
        can be sorted numerically using the number inside their name
         */
        var itemsPlusNameInts = ArrayList<Pair<Int, ApiObjectJsonItem>>()
        for(item in noNullItems)
        {
            var indexOfSpace = -1
            for(i in 0..item.name.length)
            {
                if(item.name[i] == ' ')
                {
                    indexOfSpace = i
                    break
                }
            }

            if(indexOfSpace == -1)
            {
                return backupSort(noNullItems)
            }

            var numAtEnd = item.name.subSequence(indexOfSpace + 1, item.name.length).toString()

            try
            {
                var pair = Pair(numAtEnd.toInt(), item)
                itemsPlusNameInts.add(pair)
            }
            catch (e: NumberFormatException)
            {
                return backupSort(noNullItems)
            }
        }

        //The items need to be sorted first by listId, then by name. The naive way of doing this
        //would be to make a series of sublists containing each group of listIds, sorting those,
        //and then recombine them into one list afterwards. However, since sortBy is a stable sort,
        //it is much easier to simply sort by name first, and then sort by listId, since the sort by
        //name will be preserved
        itemsPlusNameInts.sortBy { it.first }
        itemsPlusNameInts.sortBy { it.second.listId}

        var sortedResponse = ArrayList<ApiObjectJsonItem>()
        for(pair in itemsPlusNameInts)
        {
            sortedResponse.add(pair.second)
        }

        return sortedResponse
    }

    //If the naming convention were to ever change for this API, we don't want the entire app to fail.
    //So if the sorting function we have fails for any reason, this backup function kicks in
    private fun backupSort(filteredNoNull: ArrayList<ApiObjectJsonItem>) : ArrayList<ApiObjectJsonItem>
    {
        filteredNoNull.sortBy { it.name }
        filteredNoNull.sortBy { it.listId }

        return filteredNoNull
    }

    //If we fail to connect to the api on the first try, we want to be able to try again later.
    //This function will ensure we try to connect every five seconds until we either succeed, or
    //the user gives up and closes the app
    private fun attemptRequestAgain()
    {
        countDownTimer = object: CountDownTimer(5 * 1000, 1000)
        {
            override fun onTick(p0: Long)
            {
                Log.i("MainActivity", "Could not retrieve data. Will try again every five seconds.")
            }

            override fun onFinish()
            {
                makeApiRequest("")
                countDownTimer.cancel()
            }
        }
        countDownTimer.start()
    }
}