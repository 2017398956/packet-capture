package personal.nfl.networkcapture.retrofitserver;

import java.util.List;

import okhttp3.ResponseBody;
import personal.nfl.networkcapture.bean.Repo;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * @author fuli.niu
 */
public interface GitHubService {

    /**
     * get blog
     * @param user
     * @return
     */
    @GET("users/{user}/repos")
    Call<List<Repo>> listRepos(@Path("user") String user);

    // @GET("blog/{id}")
    // Call<ResponseBody> getBlog(@Path("id") int id);
}
