import sbtheader.CommentStyle
import sbtheader.HeaderPlugin.autoImport.HeaderPattern
import sbtheader.LineCommentCreator

object LicenseHeaderConfig {
    val defaultHeader = CommentStyle(
        new LineCommentCreator("/*", "*/"),
        HeaderPattern.commentBetween(raw"/\*", ".", raw"\*/")
    )

}
