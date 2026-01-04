/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.core.ui

import android.content.ClipData
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.google.samples.apps.nowinandroid.core.designsystem.R.drawable
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaIconToggleButton
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTopicTag
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.model.data.FollowableTopic
import com.google.samples.apps.nowinandroid.core.model.data.NewsResource
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toJavaZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * [NewsResource] card used on the following screens: For You, Saved
 */
// 大白话：下面是新闻卡片的 UI 组件，包含头图、标题、元信息、描述、话题标签和收藏按钮。
// 新手提示：你通常只需要改这些 Composable 的样式或 ViewModel 提供的数据。
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsResourceCardExpanded(
    userNewsResource: UserNewsResource,
    isBookmarked: Boolean,
    hasBeenViewed: Boolean,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 这个函数画整张卡片：有头图（可选）、标题、时间/类型、描述、话题标签和收藏按钮。
    // 新手可改的点：形状（RoundedCornerShape）、间距（padding）和字体样式。
    val clickActionLabel = stringResource(R.string.core_ui_card_tap_action)
    val sharingLabel = stringResource(R.string.core_ui_feed_sharing)
    val sharingContent = stringResource(
        R.string.core_ui_feed_sharing_data,
        userNewsResource.title,
        userNewsResource.url,
    )

    val dragAndDropFlags = if (VERSION.SDK_INT >= VERSION_CODES.N) {
        View.DRAG_FLAG_GLOBAL
    } else {
        0
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        // Use custom label for accessibility services to communicate button's action to user.
        // Pass null for action to only override the label and not the actual action.
        modifier = modifier
            .semantics {
                onClick(label = clickActionLabel, action = null)
            }
            .testTag("newsResourceCard:${userNewsResource.id}"),
    ) {
        Column {
            if (!userNewsResource.headerImageUrl.isNullOrEmpty()) {
                Row {
                    // 头图：会裁切为固定高度并显示加载占位
                    NewsResourceHeaderImage(userNewsResource.headerImageUrl)
                }
            }
            Box(
                modifier = Modifier.padding(16.dp),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        NewsResourceTitle(
                            userNewsResource.title,
                            modifier = Modifier
                                .fillMaxWidth((.8f))
                                .dragAndDropSource { _ ->
                                    DragAndDropTransferData(
                                        ClipData.newPlainText(
                                            sharingLabel,
                                            sharingContent,
                                        ),
                                        flags = dragAndDropFlags,
                                    )
                                },
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        // 收藏按钮（切换收藏状态）
                        BookmarkButton(isBookmarked, onToggleBookmark)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!hasBeenViewed) {
                            // 小红点：表示未读
                            NotificationDot(
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(8.dp),
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                        NewsResourceMetaData(userNewsResource.publishDate, userNewsResource.type)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    // 摘要文本，简单一段话显示内容预览
                    NewsResourceShortDescription(userNewsResource.content)
                    Spacer(modifier = Modifier.height(12.dp))
                    // 话题标签：水平滚动，注意在窄屏上标签会变小
                    NewsResourceTopics(
                        topics = userNewsResource.followableTopics,
                        onTopicClick = onTopicClick,
                    )
                }
            }
        }
    }
}

@Composable
fun NewsResourceHeaderImage(
    headerImageUrl: String?,
) {
    // 头图加载器：显示加载菊花或占位图，再显示图片
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    val imageLoader = rememberAsyncImagePainter(
        model = headerImageUrl,
        onState = { state ->
            isLoading = state is AsyncImagePainter.State.Loading
            isError = state is AsyncImagePainter.State.Error
        },
    )
    val isLocalInspection = LocalInspectionMode.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            // 加载指示器
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Crop,
            painter = if (isError.not() && !isLocalInspection) {
                imageLoader
            } else {
                painterResource(drawable.core_designsystem_ic_placeholder_default)
            },
            // decorative image,
            contentDescription = null,
        )
    }
}

@Composable
fun NewsResourceTitle(
    newsResourceTitle: String,
    modifier: Modifier = Modifier,
) {
    // 标题：大号字体，通常只改样式
    Text(newsResourceTitle,
        style = MaterialTheme.typography.headlineSmall, modifier = modifier)
}

@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 收藏切换按钮（图标在 checked/unchecked 间切换）
    NiaIconToggleButton(
        checked = isBookmarked,
        onCheckedChange = { onClick() },
        modifier = modifier,
        icon = {
            Icon(
                imageVector = NiaIcons.BookmarkBorder,
                contentDescription = stringResource(R.string.core_ui_bookmark),
            )
        },
        checkedIcon = {
            Icon(
                imageVector = NiaIcons.Bookmark,
                contentDescription = stringResource(R.string.core_ui_unbookmark),
            )
        },
    )
}

@Composable
fun NotificationDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    // 未读小圆点（仅作视觉提示）
    val description = stringResource(R.string.core_ui_unread_resource_dot_content_description)
    Canvas(
        modifier = modifier
            .semantics { contentDescription = description },
        onDraw = {
            drawCircle(
                color,
                radius = size.minDimension / 2,
            )
        },
    )
}

@Composable
fun dateFormatted(publishDate: Instant): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .withZone(LocalTimeZone.current.toJavaZoneId())
    .format(publishDate.toJavaInstant())

@Composable
fun NewsResourceMetaData(
    publishDate: Instant,
    resourceType: String,
) {
    // 显示发布时间和资源类型（如果有）
    val formattedDate = dateFormatted(publishDate)
    Text(
        if (resourceType.isNotBlank()) {
            stringResource(R.string.core_ui_card_meta_data_text, formattedDate, resourceType)
        } else {
            formattedDate
        },
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
fun NewsResourceShortDescription(
    newsResourceShortDescription: String,
) {
    // 摘要文本：简短的一段描述
    Text(newsResourceShortDescription, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun NewsResourceTopics(
    topics: List<FollowableTopic>,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 话题标签行：水平滚动，适合展示一组可点击的标签
    Row(
        // causes narrow chips
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (followableTopic in topics) {
            NiaTopicTag(
                followed = followableTopic.isFollowed,
                onClick = { onTopicClick(followableTopic.topic.id) },
                text = {
                    val contentDescription = if (followableTopic.isFollowed) {
                        stringResource(
                            R.string.core_ui_topic_chip_content_description_when_followed,
                            followableTopic.topic.name,
                        )
                    } else {
                        stringResource(
                            R.string.core_ui_topic_chip_content_description_when_not_followed,
                            followableTopic.topic.name,
                        )
                    }
                    Text(
                        text = followableTopic.topic.name.uppercase(Locale.getDefault()),
                        modifier = Modifier
                            .semantics {
                                this.contentDescription = contentDescription
                            }
                            .testTag("topicTag:${followableTopic.topic.id}"),
                    )
                },
            )
        }
    }
}

@Preview("Bookmark Button")
@Composable
private fun BookmarkButtonPreview() {
    NiaTheme {
        Surface {
            BookmarkButton(isBookmarked = false, onClick = { })
        }
    }
}

@Preview("Bookmark Button Bookmarked")
@Composable
private fun BookmarkButtonBookmarkedPreview() {
    NiaTheme {
        Surface {
            BookmarkButton(isBookmarked = true, onClick = { })
        }
    }
}

@Preview("NewsResourceCardExpanded")
@Composable
private fun ExpandedNewsResourcePreview(
    @PreviewParameter(UserNewsResourcePreviewParameterProvider::class)
    userNewsResources: List<UserNewsResource>,
) {
    CompositionLocalProvider(
        LocalInspectionMode provides true,
    ) {
        NiaTheme {
            Surface {
                NewsResourceCardExpanded(
                    userNewsResource = userNewsResources[0],
                    isBookmarked = true,
                    hasBeenViewed = false,
                    onToggleBookmark = {},
                    onClick = {},
                    onTopicClick = {},
                )
            }
        }
    }
}
