package to.tawk.githubusers.viewmodels

import androidx.lifecycle.MutableLiveData
import com.github.davidmoten.rx2.RetryWhen
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen
import to.tawk.githubusers.api.common.ApiResponse
import to.tawk.githubusers.api.common.scheduler.SchedulerProvider
import to.tawk.githubusers.api.users.service.UsersService
import to.tawk.githubusers.repository.Repository
import to.tawk.githubusers.room.entities.Details
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction
import javax.inject.Inject

@HiltViewModel
open class UserDetailsViewModel @Inject constructor(
    private val usersService: UsersService,
    private val schedulerProvider: SchedulerProvider,
    private val repository: Repository
) : BaseViewModel() {

    val userDetailsLiveData = MutableLiveData<ApiResponse<Details>>()
    var notes:String? = null

    init {
        compositeDisposable = CompositeDisposable()
    }

    /**
     * getUserDetails() triggers the request to the API and funnels the data fetched
     * to the liveData object
     * **/
    fun getUserDetails(username:String){
        val service = usersService.getUserDetails(username = username)
        compositeDisposable.add(service
            .subscribeOn(schedulerProvider.io())
                // Exponential Backoff to create delays with a factor of 4x every Retry starting from 1 sec
            .retryWhen(RetryWhen.exponentialBackoff(
                1000, TimeUnit.SECONDS, 4.0)
                .build())
            .observeOn(schedulerProvider.ui())
            .doOnSubscribe {userDetailsLiveData.value = ApiResponse.loading() }
            .subscribe({ response ->
                if (response.isSuccessful) {
                    userDetailsLiveData.value = ApiResponse.success(response = response.body())
                } else  {
                    try {
                        userDetailsLiveData.value = ApiResponse.error(response.code())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        userDetailsLiveData.value = ApiResponse.fail(e)
                    }
                }
            }, { t ->
                t.printStackTrace()
                userDetailsLiveData.value = ApiResponse.fail(t)
            }))
    }

    /**
     * gives access to the DB instance
     * */
    fun getAppDB() = repository.getAppDB()

}

