@import com.redspartan.ashlar.button.ButtonVariant
@import com.redspartan.ashlar.button.ButtonSize
@import com.redspartan.ashlar.button.ButtonStyles
@import com.redspartan.ashlar.icon.Icon
@import com.redspartan.ashlar.AshlarAssets

@param String text = ""
@param String type = "Primary"
@param String size = "Medium"
@param String buttonType = "button"
@param Icon icon = null
@param String iconPosition = "leading"
@param boolean iconOnly = false
@param String ariaLabel = ""
@param boolean disabled = false
@param boolean loading = false
@param boolean fullWidth = false
@param boolean toggle = false
@param boolean pressed = false
@param String pressedText = ""
@param String id = ""
@param String attributes = ""

!{ButtonVariant resolvedVariant = ButtonVariant.from(type);}
!{ButtonSize resolvedSize = ButtonSize.from(size);}
!{String resolvedButtonType = ButtonStyles.normalizeButtonType(buttonType);}
!{boolean hasIcon = icon != null;}
<%-- A button with an icon and no text is icon-only whether or not the caller said so explicitly. --%>
!{boolean effectiveIconOnly = iconOnly || (hasIcon && text.isEmpty());}
!{String buttonClasses = ButtonStyles.classes(resolvedVariant, resolvedSize, effectiveIconOnly, fullWidth, pressed);}
!{String iconSizeClasses = ButtonStyles.iconSizeClasses(resolvedSize);}
!{boolean showLeadingIcon = hasIcon && !loading && (effectiveIconOnly || "leading".equals(iconPosition));}
!{boolean showTrailingIcon = hasIcon && !effectiveIconOnly && !loading && "trailing".equals(iconPosition);}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String idAttr = id.isEmpty() ? null : id;}
!{String ariaBusyAttr = loading ? "true" : null;}
!{String ariaLabelAttr = effectiveIconOnly ? (ariaLabel.isEmpty() ? text : ariaLabel) : (ariaLabel.isEmpty() ? null : ariaLabel);}
!{String togglePressedAttr = toggle ? String.valueOf(pressed) : null;}
!{String toggleBaseClassesAttr = toggle ? ButtonStyles.restingClasses(resolvedVariant, resolvedSize, effectiveIconOnly, fullWidth) : null;}
!{String togglePressedClassesAttr = toggle ? ButtonStyles.pressedClasses(resolvedVariant, resolvedSize, effectiveIconOnly, fullWidth) : null;}
!{String toggleDefaultTextAttr = toggle ? text : null;}
!{String togglePressedTextAttr = toggle ? (pressedText.isEmpty() ? text : pressedText) : null;}

<button
        type="${resolvedButtonType}"
        id="${idAttr}"
        class="${buttonClasses}"
        data-btn
        data-btn-variant="${resolvedVariant.name().toLowerCase()}"
        disabled="${disabled || loading}"
        aria-busy="${ariaBusyAttr}"
        aria-label="${ariaLabelAttr}"
        data-btn-toggle="${toggle}"
        aria-pressed="${togglePressedAttr}"
        data-btn-base-classes="${toggleBaseClassesAttr}"
        data-btn-pressed-classes="${togglePressedClassesAttr}"
        data-btn-default-text="${toggleDefaultTextAttr}"
        data-btn-pressed-text="${togglePressedTextAttr}"
        $unsafe{attributes}
>
    <span
            data-btn-spinner
            class="${loading ? "inline-block" : "hidden"} h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
            aria-hidden="true"
    ></span>

    @if (showLeadingIcon)
        <span data-btn-icon aria-hidden="true">
            @template.ashlar.components.icon(icon = icon, classes = iconSizeClasses)
        </span>
    @endif

    @if (effectiveIconOnly)
        <span class="sr-only">${text}</span>
    @else
        <span data-btn-label>${text}</span>
    @endif

    @if (showTrailingIcon)
        <span data-btn-icon aria-hidden="true">
            @template.ashlar.components.icon(icon = icon, classes = iconSizeClasses)
        </span>
    @endif

    <span class="sr-only" data-btn-status aria-live="polite">${loading ? "Loading" : ""}</span>
</button>

<%-- Served from this jar's own META-INF/resources/ashlar/js/, so a consumer
     needs no build step or copy task to get it. The URL comes from
     AshlarAssets rather than being written literally, because a consumer
     deployed under a servlet context path needs that prefix - see that class
     for why a single configured prefix is the right size of mechanism. --%>
<script type="module" src="${AshlarAssets.js("button.js")}"></script>
