package com.kslimweb.rxjavatutorial

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding3.view.clicks
import com.kslimweb.rxjavatutorial.models.Post
import com.kslimweb.rxjavatutorial.models.Task
import com.kslimweb.rxjavatutorial.network.ServiceGenerator.requestApi
import com.kslimweb.rxjavatutorial.util.DataSource
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    // In general, you always want to add CompositeDisposable object
    // that can use in Rx interaction so that you can dispose those observable
    // when the Activity is destroy and you don't get memory leak
    private val compositeDisposable = CompositeDisposable()

    private lateinit var adapter: RecyclerAdapter
    private var timeSinceLastRequest by Delegates.notNull<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeSinceLastRequest = System.currentTimeMillis()

//        observableAndDisposable()

        initRecyclerView()
//        flatmapOperator() // flatmap does not maintain the order
        // execute async but with concatMap
        createObservableWithConcatMapOperator()  // concatMap maintain the order but traded with speed across all the query

        // SwitchMap() it will only a single observer exist at any given time
        // if one operation is in the middle of executing and try to execute another 1
        // it will destroy the one that was previously executing and execute the next 1
        // it make sure don't get stacking
        // Ex: Search query request but then user change the request before the first 1 completed
        // SwitchMap is the better option because you want to terminate the previous query

        // create observable with single object using Create() operator
//        createObservableWithSingleObject()

        // create observable with list of object using Create() operator
//        createObservableWithListOfObject()

        // create observable with list of object using Just operator
        // Just() operator have limits to 10 optional arrays
//        createObservableWithJustOperator()

//        createObservableWithRangeOperator()

//        createObservableWithRepeatOperator()

//        createObservableWithMapOperator()

//        createObservableWithBufferOperator()

//        createObservableWithRxBindingAndBufferOperator()

//        Debounce will keep on waiting till the items keep on coming and
//        only emit an item when there is a pause (for given time).
//        createObservableWithDebounceOperator()

        // ThrottleFirst will just take the first item and will discard all other items for a specified time.
//        createObservableWithThrottleFirstOperator()

    }

    @SuppressLint("CheckResult")
    private fun createObservableWithMapOperator() {
        Observable
            .fromIterable(DataSource.createTasksList())
            .map {
                // map to string
                return@map it.description
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                Log.d(TAG, "onNext: " + it)
            }
            .subscribe({

            }, { })

    }

    @SuppressLint("CheckResult")
    private fun createObservableWithConcatMapOperator() {
        // using flatmap operator
        getPostsObservable()
            // no need to change thread since getPostsObservable() changed the thread
//            .subscribeOn(Schedulers.io())
            .concatMap {
                getCommentsObservable(it)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                compositeDisposable.add(it)
            }
            .doOnNext {
                updatePost(it)
            }
            .subscribe({}, {
                Log.e(TAG, "onError: ", it)
            })
    }

    @SuppressLint("CheckResult")
    private fun createObservableWithThrottleFirstOperator() {
        button.clicks()
            .throttleFirst(4000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe{ compositeDisposable.add(it) }
            .doOnNext {
                Log.d(TAG, "onNext: Since last request " + (System.currentTimeMillis() - timeSinceLastRequest))
                // someMethod() to perform some simulating
            }
            .subscribe({}, {})
    }

    @SuppressLint("CheckResult")
    private fun createObservableWithDebounceOperator() {
        // observe changes in the search view
        // i want capture any changes to the search view
        // and after 500 milliseconds after the last character was pressed
        // start emitting /  perform the search
        val observable = Observable.create(ObservableOnSubscribe<String> {
            search_view.setOnQueryTextListener(object: SearchView.OnQueryTextListener,
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (!it.isDisposed)
                        it.onNext(newText!!)
//                    newText?.let { text -> it.onNext(text) }
                    return false
                }
            })
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(1000, TimeUnit.MILLISECONDS)

        observable.doOnSubscribe { compositeDisposable.add(it) }
            .doOnNext{
                Log.d(TAG, "onNext: Search Query: " + it)
                Log.d(TAG, "onNext: Since last request " + (System.currentTimeMillis() - timeSinceLastRequest))
                // sendRequestToServer(it)
            }
            .subscribe({ },{ })
    }

    @SuppressLint("CheckResult")
    private fun createObservableWithRxBindingAndBufferOperator() {
        // detect the clicks. Everytime there is a click return 1
        // collect all those clicks for 4 seconds
        button.clicks()
            .map { return@map 1 }
            .buffer(4, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe{ compositeDisposable.add(it) }
            .doOnNext {
                // {1,1,1,1,1}
                Log.d(TAG, "onNext: You clicked " + it.size + " times in 4 seconds")
            }
            .subscribe({}, {})
    }

    @SuppressLint("CheckResult")
    private fun createObservableWithBufferOperator() {
        val task = Observable
            .fromIterable(DataSource.createTasksList())
            .subscribeOn(Schedulers.io())

        task.buffer(2)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                it.forEach {
                    Log.d(TAG, "onNext: " + it.description)
                }
            }
            .subscribe({}, {})
    }

    private fun observableAndDisposable() {
        // everytime you subscribe observer on observable, it will return disposable
//         regardless what you use to subscribe on
        val disposable = Observable.fromIterable(DataSource.createTasksList())
            .subscribeOn(Schedulers.io())
            .filter{
                // this does not freeze the UI as it runs on background thread
                println("Filter -> " + Thread.currentThread().name)
                Thread.sleep(1000)
                it.isComplete
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                // what happen to observer when something is finish that subscribed to observable
                // -> No longer needed. Disposable keep track all the observers that using and clean them up
                println("Subscribed. OnNext -> " + Thread.currentThread().name + ", " + it.description)
//                Thread.sleep(1000) // this will sleep the main thread and UI will be freeze
            }, {
                println("Something goes wrong. " + it.message)
            })

        compositeDisposable.add(disposable)
        disposable.addTo(compositeDisposable) //Alternatively, use this RxKotlin extension function.

//         IF MVVM, we need to keep track where is the Disposable in the ViewModel.
        compositeDisposable.dispose() //Placed wherever we'd like to dispose our Disposables (i.e. in onDestroy())
    }

    @SuppressLint("CheckResult")
    private fun createObservableWithRepeatOperator() {
        val observable = Observable
            .range(0, 3)
            .repeat(3)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

        observable.doOnNext {
            Log.d(TAG, "onNext: " + it)
        }.subscribe { }
    }

    @SuppressLint("CheckResult")
    private fun createObservableWithRangeOperator() {
//        val observable = Observable
//            .range(0, 9)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())

        val observable = Observable
            .range(0, 9)
            .map {
                // takes integer as input and map it into task
                Log.d(TAG,"Apply: " + Thread.currentThread().name)
                return@map Task("Task with priority " + it,
                    false,
                    it)
            }
            .takeWhile {
                // keep emitting task until priority reaches 9
                return@takeWhile it.priority < 9
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

        observable.doOnNext {
//            Log.d(TAG, "onNext: " + it)
            Log.d(TAG, "onNext: " + it.priority)
        }.subscribe { }
    }

    @SuppressLint("CheckResult")
    private fun createObservableWithJustOperator() {
        val task = Task("Walk the dog", false, 3)

        val taskObservable = Observable
            .just(task, task, task)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

        taskObservable.doOnNext {
            Log.d(TAG, "onNext: " + it.description)
        }.subscribe {}
    }

    @SuppressLint("CheckResult")
    private fun createObservableWithListOfObject() {
        val taskObservable = Observable.create(
            ObservableOnSubscribe<Task> {
                // here we create the observable
                DataSource.createTasksList().forEach { task ->
                    if (!it.isDisposed)
                        it.onNext(task)
                }
                if (!it.isDisposed)
                    it.onComplete()
            }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

        taskObservable.doOnNext {
            Log.d(TAG, "onNext: " + it.description)
        }.subscribe {}
    }

    @SuppressLint("CheckResult")
    private fun createObservableWithSingleObject() {
        val task = Task("Walk the dog", false, 3)

        val taskObservable = Observable.create(
            ObservableOnSubscribe<Task> {
                // here we create the observable
                if (!it.isDisposed) {
                    it.onNext(task)
                    it.onComplete()
                }
            }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

        taskObservable.doOnNext {
            Log.d(TAG, "onNext: " + it.description)
        }.subscribe {}
    }

    @SuppressLint("CheckResult")
    private fun flatmapOperator() {
        // using flatmap operator
        getPostsObservable()
            // no need to change thread since getPostsObservable() changed the thread
//            .subscribeOn(Schedulers.io())
            .flatMap {
                getCommentsObservable(it)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                compositeDisposable.add(it)
            }
            .doOnNext {
                updatePost(it)
            }
            .subscribe({}, {
                Log.e(TAG, "onError: ", it)
            })
    }

    private fun updatePost(post: Post) {
        adapter.updatePost(post)
    }

    private fun getCommentsObservable(post: Post): Observable<Post> {
        return requestApi
            .getComments(post.id)
            .map {
                val delay: Int = (Random().nextInt(5) + 1) * 1000 // sleep thread for x ms
                Thread.sleep(delay.toLong())
                Log.d(
                    TAG,
                    "apply: sleeping thread " + Thread.currentThread().name + " for " + delay.toString() + "ms"
                )
                post.comments = it
                return@map post
            }
            .subscribeOn(Schedulers.io())
    }

    private fun getPostsObservable(): Observable<Post> {
        return requestApi
            .getPosts()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap<Post> {
                adapter.setPosts(it as MutableList<Post>)
                Observable.fromIterable(it).subscribeOn(Schedulers.io())
            }
    }

    private fun initRecyclerView() {
        adapter = RecyclerAdapter()
        recycler_view.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear() // this will remove all the subscribers and observers
//        compositeDisposable.dispose() // will no longer allow any thing to subscribe to observable. Disable it
    }
}
