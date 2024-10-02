import de.heikoseeberger.sbtheader.LineCommentCreator
import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern
object LicenseHeaderConfig {
    val defaultHeader = CommentStyle(new LineCommentCreator("/*", "*/"),
        HeaderPattern.commentBetween(raw"/\*", ".", raw"\*/"))

}