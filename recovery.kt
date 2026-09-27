            } // close Box for contentRef
            
            // Swipe up from the very bottom of the screen to open the full-screen menu
            val bottomDensity = androidx.compose.ui.platform.LocalDensity.current
            val bottomThresholdPx = with(bottomDensity) { 45.dp.toPx() }
            val bottomAnim = remember { Animatable(0f) }
            val bottomProgress = (abs(bottomAnim.value) / bottomThresholdPx).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .constrainAs(bottomTriggerRef) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .pointerInput(showMenu, currentUrl) {
                        if (showMenu || currentUrl == "pixelbrowser://home") return@pointerInput
                        detectVerticalDragGestures(
                            onDragStart = {
                                coroutineScope.launch {
                                    bottomAnim.snapTo(0f)
                                }
                            },
                            onDragEnd = {
                                if (bottomAnim.value < -bottomThresholdPx) {
                                    showMenu = true
                                }
                                coroutineScope.launch {
                                    bottomAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    bottomAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                coroutineScope.launch {
                                    bottomAnim.snapTo((bottomAnim.value + dragAmount).coerceAtMost(0f))
                                }
                            }
                        )
                    }
            )

            // Bottom slide-up visual indicator
            if (bottomProgress > 0f) {
                Box(
                    modifier = Modifier
                        .constrainAs(bottomIndicatorRef) {
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .offset(y = (40 - (bottomProgress * 40)).dp)
                        .size(width = 72.dp, height = 48.dp)
                        .alpha(bottomProgress)
                        .scale(0.8f + bottomProgress * 0.2f)
                        .background(
                            color = if (bottomProgress >= 1f)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Open Menu",
                        tint = if (bottomProgress >= 1f)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = (-4).dp)
                    )
                }
            }

            if (screenState == ScreenState.HOME || screenState == ScreenState.HISTORY) {
                val activePage = if (screenState == ScreenState.HOME) 0 else 1
                PageIndicator(
                    activePage = activePage,
                    pageCount = 2,
                    onPageSelected = { index ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (index == 0) {
                            viewModel.navigateTo("pixelbrowser://home")
                        } else if (index == 1) {
                            viewModel.navigateTo("pixelbrowser://history")
                        }
                    },
                    modifier = Modifier
                        .constrainAs(pageIndicatorRef) {
                            bottom.linkTo(parent.bottom, margin = 12.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                )
            }
        }

        SearchDialog(
            show = showSearchDialog,
            onDismiss = { showSearchDialog = false },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            searchHistory = searchHistory,
            onAddSearchHistory = { viewModel.addSearchHistory(it) },
            onDeleteSearchHistory = { viewModel.removeSearchHistory(it) },
            onNavigate = { viewModel.navigateTo(it) }
        )

        QrDialog(
            show = showQrDialog,
            onDismiss = { showQrDialog = false },
            currentUrl = currentUrl
        )
    }
}
