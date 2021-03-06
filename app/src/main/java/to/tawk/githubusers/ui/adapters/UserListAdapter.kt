package to.tawk.githubusers.ui.adapters;

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.CropCircleTransformation
import jp.wasabeef.glide.transformations.gpu.InvertFilterTransformation
import to.tawk.githubusers.databinding.LayoutItemRowBinding
import to.tawk.githubusers.room.entities.User
import to.tawk.githubusers.ui.UserDetailsActivity
import to.tawk.githubusers.viewmodels.UsersViewModel

class UserListAdapter (private val context: Activity,
                       private val viewModel: UsersViewModel,
                       private val launcher: ActivityResultLauncher<Intent>
                       ) : PagingDataAdapter<User, UserListAdapter.VHolder>(DIFF_CALLBACK) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHolder {
        val binding = LayoutItemRowBinding.inflate(LayoutInflater.from(context), parent, false)
        return VHolder(binding,viewModel,context,launcher)
    }

    override fun onBindViewHolder(viewHolder: VHolder, position: Int) {
        val item = getItem(position)!!
        val isInverted = (position + 1) % 4 == 0  // invert colors of image on every fourth item
        viewHolder.bind(item, isInverted)
    }

    class VHolder (private val layoutUserBinding: LayoutItemRowBinding,
                   val viewModel: UsersViewModel,
                   val context: Activity,
                   val launcher: ActivityResultLauncher<Intent>
                   ) : RecyclerView.ViewHolder(layoutUserBinding.root) {
        fun bind(item: User, isInverted: Boolean){

            layoutUserBinding.tvName.text = "${item.login}"
            layoutUserBinding.tvDetails.text = "${item.html_url}"

            Glide.with(itemView.context).load(item?.avatar_url)
                .apply {
                    val multiTransformation = MultiTransformation (CropCircleTransformation(),InvertFilterTransformation())
                    if (isInverted)
                        apply(RequestOptions.bitmapTransform(multiTransformation))
                    else
                        apply(RequestOptions.bitmapTransform(CropCircleTransformation()))
                }
                .into(layoutUserBinding.imgPic)

            layoutUserBinding.root.setOnClickListener {
                val i = Intent(itemView.context,UserDetailsActivity::class.java)
                i.putExtra("user_item",item)
                i.putExtra("is_inverted",isInverted)
                launcher.launch(i)
            }
            val details = viewModel.getAppDB().userDetailsDao().getUsersDetail(item.login)
            layoutUserBinding.imgNote.isVisible = !details?.note.isNullOrEmpty() == true
        }
    }
    companion object {
        var DIFF_CALLBACK: DiffUtil.ItemCallback<User> = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.id === newItem.id
            }
            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
    }
}
