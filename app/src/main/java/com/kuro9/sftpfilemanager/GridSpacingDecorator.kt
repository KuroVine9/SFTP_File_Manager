package com.kuro9.sftpfilemanager

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 그리드 카드 간 여백 주기 위한 클래스
 * @param space 간격
 * @param spanCount 카드의 열 수
 */
class GridSpacingDecorator(private val space: Int, private val spanCount: Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        outRect.left = space - column * space / spanCount
        outRect.right = (column + 1) * space / spanCount

        if (position >= spanCount) {
            outRect.top = space
        }
    }
}