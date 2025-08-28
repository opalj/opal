import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern
import de.heikoseeberger.sbtheader.LineCommentCreator

object LicenseHeaderConfig {
    val defaultHeader = CommentStyle(
        new LineCommentCreator("/*", "*/"),
        HeaderPattern.commentBetween(raw"/\*", ".", raw"\*/")
    )

}
