package mobile.agentplatform

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_directory_chooser.*
import java.io.File

class DirectoryChooserFragment : Fragment(), AdapterView.OnItemClickListener {
    private var listener: OnFragmentInteractionListener? = null
    private var listFiles: MutableList<AppFile> = mutableListOf()
    private lateinit var adapter: ArrayAdapter<AppFile>
    private lateinit var fileSystem: FileSystem
    private var selectedDir: AppFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileSystem = FileSystem(context!!)
        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, listFiles)
        listFiles.add(AppFile(fileSystem.getExternalStorageDir()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_directory_chooser, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btnSelect.setOnClickListener {
            if (selectedDir == null) {
                Toast.makeText(context, R.string.null_selected_dir, Toast.LENGTH_SHORT).show()
            } else if (!selectedDir!!.canWrite()) {
                Toast.makeText(context, R.string.not_writable_selected_dir, Toast.LENGTH_LONG).show()
            } else {
                listener?.onDirectoryChoose(selectedDir!!)
            }
        }

        listViewFiles.onItemClickListener = this
        listViewFiles.adapter = adapter
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        var dir = listFiles[position]
        listFiles.clear()

        if (dir.isPointer) {
            dir = AppFile(dir.parent, true)
            if (dir.parentFile.canRead()) {
                listFiles.add(dir)
            }

        } else {
            listFiles.add(AppFile(dir.absolutePath, true))
        }

        if (dir.canRead()) {
            for (file in dir.listFiles()) {
                if (file.isDirectory) {
                    listFiles.add(AppFile(file.absolutePath))
                }
            }
        }
        selectedDir = dir
        txtDirLoc.text = selectedDir?.absolutePath
        viewPath.visibility = View.VISIBLE

        adapter.notifyDataSetChanged()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onDirectoryChoose(file: File)
    }
}
