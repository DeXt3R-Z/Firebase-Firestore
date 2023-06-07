package com.example.firebasefirestore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.firebasefirestore.databinding.ActivityMainBinding
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var curVal = 0L


    private val firestore = Firebase.firestore

    private val firestoreCollection = firestore.collection("person")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Code to fetch the data and display it on activity creation (TRANSACTION)
        firestoreCollection.document("Counter").get().addOnSuccessListener {
            if(it!=null)
            {
                val data = it.getLong("currentValue")
                binding.tvShowValue.text = data.toString()
                Log.d("TAG", "DocumentSnapshot data: ${it.data}")
            } else {
                Log.d("TAG", "No such document")
            }
        }.addOnFailureListener{
            Toast.makeText(this,it.message,Toast.LENGTH_SHORT).show()
        }

        binding.btnSubmit.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if (binding.etName.text.toString()!="" && binding.etLastName.text.toString()!="" && binding.etAge.text.toString()!="")
                {
                    addData(Person(binding.etName.text.toString(),binding.etLastName.text.toString(),Integer.parseInt(binding.etAge.text.toString())))
                }
            }
        }

        binding.btnRetrieve.setOnClickListener {
            if(binding.etStartAge.text.toString() == "" || binding.etStartAge.text.toString() == "")
            {
                CoroutineScope(Dispatchers.IO).launch{
                    getData(firestoreCollection.get().await()) //getting all the data if no initial or last age is mentioned
                }
            }
            else
            {
                CoroutineScope(Dispatchers.IO).launch {
                    //querying the data if the initial and the last age is mentioned
                    getData(firestoreCollection.whereGreaterThan("age",Integer.parseInt(binding.etStartAge.text.toString()))
                        .whereLessThan("age",Integer.parseInt(binding.etEndAge.text.toString())).get().await())
                }
            }
        }

        binding.btnUpdate.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                updateData(newPersonData())
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity,"Data Updated",Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnAdd.setOnClickListener {
            counterIncr()
        }

        binding.btnDeduct.setOnClickListener {
            counterDecr()
        }


    }
    //End of onCreate() and beginning of firestore operation methods


    /***************************************/
    /***************************************/
    /************* ADDING DATA *************/
    /***************************************/
    /***************************************/

    //Add data to the collection
    fun addData(p: Person)
    {
        firestoreCollection.document(binding.etName.text.toString()).set(p).addOnSuccessListener {
            Toast.makeText(this@MainActivity,"Successfully written",Toast.LENGTH_SHORT).show()
        }
    }


    /***************************************/
    /***************************************/
    /********** FETCHING THE DATA **********/
    /***************************************/
    /***************************************/

    //get the data using the addOnSuccessListener and the addOnFailureListener as shown in the documentation
    fun getDataDocumentation(){
        val sb = StringBuilder()
        firestoreCollection.get().addOnSuccessListener {
            for(document in it.documents)
            {
                val person = document.toObject<Person>()
                sb.append("$person\n")
            }
        }.addOnFailureListener{
            Toast.makeText(this@MainActivity,"Failed to retrieve data",Toast.LENGTH_SHORT).show()
        }
    }

    //get data as shown by Philip Lackner
    fun getData(querySnapshot: QuerySnapshot) {
        CoroutineScope(Dispatchers.IO).launch{
            try {
                val builder = StringBuilder()
                for(document in querySnapshot.documents)
                {
                    val person =  document.toObject<Person>()!!
                    builder.append("$person\n")
                }
                withContext(Dispatchers.Main)
                {
                    binding.tvValues.text = builder.toString()
                }
            }catch (e: Exception)
            {
                withContext(Dispatchers.Main)
                {
                    Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //fetching the data in realtime
    fun getRealTimeData()
    {
        try {
            firestoreCollection.addSnapshotListener { querySnapshot, error ->
                if(error!=null)
                {
                    Toast.makeText(this@MainActivity,error.message,Toast.LENGTH_SHORT).show()
                }
                if(querySnapshot!=null)
                {
                    val sb = StringBuilder()
                    for(document in querySnapshot.documents)
                    {
                        val person = document.toObject(Person::class.java)
                        sb.append("$person\n")
                    }
                    binding.tvValues.text = sb.toString()
                }
            }
        }catch (e: Exception)
        {

        }
    }


    /***************************************/
    /***************************************/
    /*** DATA UPDATION by PHILIP LACKNER ***/
    /***************************************/
    /***************************************/

    //Creating a map the update the data even if some fields are left empty
    fun newPersonData(): Map<String,Any>
    {
        var name = binding.etNewName.text.toString()
        var lastName = binding.etNewLastName.text.toString()
        var age = binding.etNewAge.text.toString()
        val map = mutableMapOf<String,Any>()
        if (name != "")
        {
            map["name"] = name
        }
        if(lastName != "")
        {
            map["lastName"] = lastName
        }
        if(age != "")
        {
            map["age"] = age.toInt()
        }
        return map
    }

    //Data updation as shown by Philip Lackner
    suspend fun updateData(map : Map<String,Any>)
    {
        val querySnapshot = firestoreCollection
            .whereEqualTo("name",binding.etName.text.toString())
            .whereEqualTo("lastName",binding.etLastName.text.toString())
            .whereEqualTo("age",binding.etAge.text.toString().toInt())
            .get().await()

        if(querySnapshot.documents.isNotEmpty())
        {
            for (doc in querySnapshot.documents)
            {
                try {

                    firestoreCollection.document(doc.id).set(map, SetOptions.merge()).await()
                }catch (e : Exception)
                {
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        else{
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,"No person found",Toast.LENGTH_SHORT).show()
            }
        }
    }


    /***************************************/
    /***************************************/
    /************* BATCH WRITE *************/
    /***************************************/
    /***************************************/

    //Batch Writing
    fun batchWrite()
    {
        firestore.runBatch {
            val batch = firestoreCollection.document("Krishna")
            it.set(batch,Person("Shiva","Mahadeva",999999999))
        }.addOnSuccessListener {
            Toast.makeText(this,"Batch Writing successful",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this,it.message,Toast.LENGTH_SHORT).show()
        }
    }


    /***************************************/
    /***************************************/
    /************* TRANSACTION *************/
    /***************************************/
    /***************************************/

    //Transaction example 1
    fun counterIncr()
    {
        firestore.runTransaction { transaction ->
            val document = firestoreCollection.document("Counter")
            var docSnap = transaction.get(document)
            var incNum =  docSnap["currentValue"] as Long +1
            //curVal = incNum
            transaction.update(document,"currentValue",incNum)
            incNum
        }.addOnSuccessListener {
            binding.tvShowValue.text = it.toString()
            Toast.makeText(this,"Successfully increased the value",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this,it.message,Toast.LENGTH_SHORT).show()
        }
    }
    //Transaction example 2
    fun counterDecr()
    {
        firestore.runTransaction {
            val document = firestoreCollection.document("Counter")
            val snap = it.get(document)
            val decNum = snap.getLong("currentValue")!! - 1
            //curVal = decNum
            it.update(document,"currentValue",decNum)
            decNum
        }.addOnSuccessListener {
            binding.tvShowValue.text = it.toString()
            Toast.makeText(this,"Successfully increased the value",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this,it.message,Toast.LENGTH_SHORT).show()
        }
    }


}