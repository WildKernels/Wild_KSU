package com.rifsxd.ksunext.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.util.LocalUiOverlaySettings

@Composable
fun IssueReportCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val githubIssueUrl = stringResource(R.string.issue_report_github_link)
    val telegramUrl = stringResource(R.string.issue_report_telegram_link)
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.issue_report_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body_2),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = { uriHandler.openUri(githubIssueUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = stringResource(R.string.issue_report_github),
                    )
                }
                IconButton(onClick = { uriHandler.openUri(telegramUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_telegram),
                        contentDescription = stringResource(R.string.issue_report_telegram),
                    )
                }
            }
        }
    }
}
