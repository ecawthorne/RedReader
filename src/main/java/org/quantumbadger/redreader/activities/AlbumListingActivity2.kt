/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.activities

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ProgressBar
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.isSensitiveDebugLoggingEnabled
import org.quantumbadger.redreader.common.General.isThisUIThread
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.LinkHandler.getAlbumInfo
import org.quantumbadger.redreader.common.LinkHandler.getImgurImageInfo
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.compose.activity.ComposeBaseActivity
import org.quantumbadger.redreader.compose.ui.AlbumScreen
import org.quantumbadger.redreader.image.AlbumInfo
import org.quantumbadger.redreader.image.GetAlbumInfoListener
import org.quantumbadger.redreader.image.GetImageInfoListener
import org.quantumbadger.redreader.image.ImageInfo
import org.quantumbadger.redreader.views.liststatus.ErrorView

class AlbumListingActivity2 : ComposeBaseActivity() {
	private var mUrl: String? = null
	private var mHaveReverted = false

	override fun onCreate(savedInstanceState: Bundle?) {
		PrefsUtility.applyTheme(this)

		super.onCreate(savedInstanceState)

		setTitle(R.string.image_gallery)

		val intent = intent

		mUrl = intent.dataString

		if (mUrl == null) {
			finish()
			return
		}

		if (isSensitiveDebugLoggingEnabled) {
			Log.i("AlbumListingActivity", "Loading URL $mUrl")
		}

		val progressBar = ProgressBar(
			this,
			null,
			android.R.attr.progressBarStyleHorizontal
		)
		progressBar.isIndeterminate = true

		val layout = LinearLayout(this)
		layout.orientation = LinearLayout.VERTICAL
		layout.addView(progressBar)

		getAlbumInfo(
			this,
			mUrl!!,
			Priority(Constants.Priority.IMAGE_VIEW),
			object : GetAlbumInfoListener {
				override fun onGalleryRemoved() {
					AndroidCommon.UI_THREAD_HANDLER.post {
						layout.removeAllViews()
						layout.addView(
							ErrorView(
								this@AlbumListingActivity2,
								RRError(
									applicationContext.getString(
										R.string.image_gallery_removed_title
									),
									applicationContext.getString(
										R.string.image_gallery_removed_message
									),
									true,
									null,
									null,
									mUrl,
									null
								)
							)
						)
					}
				}

				override fun onGalleryDataNotPresent() {
					AndroidCommon.UI_THREAD_HANDLER.post {
						layout.removeAllViews()
						layout.addView(
							ErrorView(
								this@AlbumListingActivity2,
								RRError(
									applicationContext.getString(
										R.string.image_gallery_no_data_present_title
									),
									applicationContext.getString(
										R.string.image_gallery_no_data_present_message
									),
									true,
									null,
									null,
									mUrl,
									null
								)
							)
						)
					}
				}

				override fun onFailure(error: RRError) {
					Log.e(
						"AlbumListingActivity",
						"getAlbumInfo call failed: $error"
					)

					if (error.httpStatus == null) {
						revertToWeb()
						return
					}

					// It might be a single image, not an album
					val matchImgur = LinkHandler.imgurAlbumPattern.matcher(mUrl)

					if (matchImgur.find()) {
						val albumId = matchImgur.group(2)

						getImgurImageInfo(
							this@AlbumListingActivity2,
							albumId,
							Priority(Constants.Priority.IMAGE_VIEW),
							false,
							object : GetImageInfoListener {
								override fun onFailure(error: RRError) {
									Log.e(
										"AlbumListingActivity",
										"Image info request also failed: "
												+ error
									)
									revertToWeb()
								}

								override fun onSuccess(info: ImageInfo) {
									if (info.original != null) {
										Log.i(
											"AlbumListingActivity",
											"Link was actually an image."
										)
										onLinkClicked(
											this@AlbumListingActivity2,
											info.original.url
										)
										finish()
									} else {
										revertToWeb()
									}
								}

								override fun onNotAnImage() {
									Log.i(
										"AlbumListingActivity",
										"Not an image either"
									)
									revertToWeb()
								}
							})
					} else {
						Log.e(
							"AlbumListingActivity",
							"Not an imgur album, not checking for single image"
						)
						revertToWeb()
					}
				}

				override fun onSuccess(info: AlbumInfo) {
					if (isSensitiveDebugLoggingEnabled) {
						Log.i(
							"AlbumListingActivity",
							"Got album, " + info.images.size + " image(s)"
						)
					}

					AndroidCommon.UI_THREAD_HANDLER.post {
						if (info.title != null && !info.title.trim { it <= ' ' }.isEmpty()) {
							title = (getString(R.string.image_gallery)
									+ ": "
									+ info.title)
						}

						if (info.images.size == 1) {
							onLinkClicked(
								this@AlbumListingActivity2,
								info.images[0].original!!.url
							)
							finish()
						} else {

							setContentCompose {
								AlbumScreen(album = info)
							}
						}
					}
				}
			})

		setBaseActivityListing(layout)
	}

	override fun onBackPressed() {
		if (General.onBackPressed()) {
			super.onBackPressed()
		}
	}

	private fun revertToWeb() {
		val r = Runnable {
			if (!mHaveReverted) {
				mHaveReverted = true
				onLinkClicked(this, mUrl, true)
				finish()
			}
		}

		if (isThisUIThread) {
			r.run()
		} else {
			AndroidCommon.UI_THREAD_HANDLER.post(r)
		}
	}
}
