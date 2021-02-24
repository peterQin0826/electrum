package org.haobtc.onekey.bean

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.google.common.base.Objects

interface ImageResources {
  fun intoTarget(imageView: ImageView)
}

class LocalImage(@DrawableRes val res: Int) : ImageResources {
  override fun intoTarget(imageView: ImageView) {
    imageView.setImageDrawable(ResourcesCompat.getDrawable(imageView.resources, res, null))
  }

  override fun hashCode(): Int {
    return Objects.hashCode(res)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (hashCode() != other.hashCode()) return false
    return true
  }
}

class RemoteImage(val url: String) : ImageResources {
  override fun intoTarget(imageView: ImageView) {
    Glide.with(imageView.context).load(url).into(imageView)
  }

  override fun hashCode(): Int {
    return Objects.hashCode(url)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (hashCode() != other.hashCode()) return false
    return true
  }
}
