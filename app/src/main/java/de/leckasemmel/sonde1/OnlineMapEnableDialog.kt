package de.leckasemmel.sonde1

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import de.leckasemmel.sonde1.databinding.DialogOnlineMapEnableBinding
import de.leckasemmel.sonde1.viewmodels.DialogOnlineMapEnableViewModel


class OnlineMapEnableDialog(
            private val ctx: Context,
            val viewModel: DialogOnlineMapEnableViewModel,
            private val okButtonListener: View.OnClickListener
) : Dialog(ctx) {
    private lateinit var binding: DialogOnlineMapEnableBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogOnlineMapEnableBinding.inflate(LayoutInflater.from(ctx))
        binding.viewModel = viewModel
        setContentView(binding.root)

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        binding.buttonOk.setOnClickListener { v ->
            okButtonListener.onClick(v)
            dismiss()
        }
    }
}
