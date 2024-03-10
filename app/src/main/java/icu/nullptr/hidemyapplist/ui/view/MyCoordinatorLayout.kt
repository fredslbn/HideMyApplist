package icu.nullptr.hidemyapplist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.coordinatorlayout.widget.CoordinatorLayout

class MyCoordinatorLayout : CoordinatorLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var keyPreImeCallback: ((KeyEvent) -> Boolean)? = null

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (keyPreImeCallback?.invoke(event) == true) return true
        return super.dispatchKeyEventPreIme(event)
    }
}
