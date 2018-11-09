package personal.nfl.networkcapture.utils;

import android.content.Context;
import android.widget.Toast;

import java.util.List;

import personal.nfl.networkcapture.bean.Repo;
import personal.nfl.networkcapture.retrofitserver.GitHubService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by nfl 2018/11/9 11:15
 */
public class TestUtil {
    public static void testRetrofit(Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GitHubService service = retrofit.create(GitHubService.class);
        Call<List<Repo>> call = service.listRepos("octocat");
        call.enqueue(new Callback<List<Repo>>() {
                         @Override
                         public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {
                             Toast.makeText(context, "----", Toast.LENGTH_SHORT).show();
                         }

                         @Override
                         public void onFailure(Call<List<Repo>> call, Throwable t) {
                             Toast.makeText(context, "====", Toast.LENGTH_SHORT).show();
                         }
                     }
        );
    }
}
