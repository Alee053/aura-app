package com.programovil.aura.auth.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.auth.domain.AuthError
import com.programovil.aura.designsystem.components.state.AuraInlineNotice
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.shared.presentation.composable.*
import org.jetbrains.compose.resources.*

@Composable
fun authErrorMessage(error: AuthError): String = when(error) {
    is AuthError.NoCredential -> stringResource(Res.string.auth_error_no_credential)
    is AuthError.NoToken -> stringResource(Res.string.auth_error_no_token)
    is AuthError.Exception -> stringResource(Res.string.rd_auth_error)
}
@Composable
fun SignInScreen(errorMessage: String?, onSignInClick: () -> Unit, isLoading: Boolean = false) {
    Box(Modifier.fillMaxSize().background(AppTheme.colors.background).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = AuraSpacing.editorWidth).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(AuraSpacing.lg), verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuraBrandMark(Modifier.size(32.dp))
                Text(stringResource(Res.string.app_name_label), Modifier.padding(start = AuraSpacing.sm), style = AppTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(AuraSpacing.xs))
            Text(stringResource(Res.string.rd_intro_title), style = AppTheme.typography.headlineLarge)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AuraScene(4, Modifier.width(232.dp))
            }
            Text(stringResource(Res.string.rd_intro_body), style = AppTheme.typography.bodyLarge, color = AppTheme.colors.textSecondary)
            OutlinedButton(onSignInClick, Modifier.fillMaxWidth().heightIn(min = AuraSpacing.control), enabled = !isLoading,
                shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFF747775)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F),
                    disabledContainerColor = Color.White, disabledContentColor = Color(0xFF1F1F1F))) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF1F1F1F), strokeWidth = 2.dp)
                else Image(painterResource(Res.drawable.google_g), null, Modifier.size(20.dp))
                Spacer(Modifier.width(AuraSpacing.sm))
                Text(stringResource(Res.string.sign_in_button),
                    style = AppTheme.typography.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium))
            }
            errorMessage?.let { AuraInlineNotice(it) }
        }
    }
}
