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

package org.quantumbadger.redreader.reddit.prepared.bodytext;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.activities.ViewsBaseActivity;
import org.quantumbadger.redreader.common.General;

public class BodyElementSpoilerButton extends BodyElementBaseButton {

	@NonNull private final BodyElement mSpoilerText;

	public BodyElementSpoilerButton(
			@NonNull final AppCompatActivity activity,
			@NonNull final BodyElement spoilerText) {

		super(activity.getApplicationContext().getString(R.string.spoiler), null, false);
		mSpoilerText = spoilerText;
	}

	@NonNull
	@Override
	protected View.OnClickListener generateOnClickListener(
			@NonNull final ViewsBaseActivity activity,
			@Nullable final Integer textColor,
			@Nullable final Float textSize,
			final boolean showLinkButtons) {

		return (button) -> {
			final ScrollView scrollView = new ScrollView(activity);

			final View view = mSpoilerText.generateView(
					activity,
					textColor,
					textSize,
					true);

			scrollView.addView(view);

			final ViewGroup.MarginLayoutParams layoutParams
					= (FrameLayout.LayoutParams)view.getLayoutParams();

			final int marginPx = General.dpToPixels(activity, 14);
			layoutParams.setMargins(marginPx, marginPx, marginPx, marginPx);

			final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
			builder.setView(scrollView);

			builder.setNeutralButton(
					R.string.dialog_close,
					(dialog, which) -> {
					});

			final AlertDialog alert = builder.create();
			alert.show();
		};
	}

	@Nullable
	@Override
	protected View.OnLongClickListener generateOnLongClickListener(
			@NonNull final ViewsBaseActivity activity,
			@Nullable final Integer textColor,
			@Nullable final Float textSize,
			final boolean showLinkButtons) {

		return null;
	}
}
